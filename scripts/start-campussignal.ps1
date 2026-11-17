param(
    [int]$FrontendPort = 5174,
    [int]$BackendPort = 8092,
    [switch]$NoBrowser,
    [switch]$Watch
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$frontendRoot = Join-Path $root 'frontend'
$jar = Join-Path $root 'target\campussignal-0.1.0-SNAPSHOT.jar'
$logRoot = Join-Path $root 'target\logs'

function Test-PortListening([int]$Port) {
    return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Test-ExpectedStatus([string]$Uri, [int[]]$ExpectedStatus) {
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $Uri -TimeoutSec 3
        return $ExpectedStatus -contains [int]$response.StatusCode
    } catch {
        $response = $_.Exception.Response
        if ($null -eq $response) {
            return $false
        }
        return $ExpectedStatus -contains [int]$response.StatusCode
    }
}

function Wait-ForBackend([System.Diagnostics.Process]$StartedProcess) {
    $sessionUri = "http://127.0.0.1:$BackendPort/api/v1/auth/session"
    for ($attempt = 1; $attempt -le 45; $attempt++) {
        if ($null -ne $StartedProcess) {
            $StartedProcess.Refresh()
            if ($StartedProcess.HasExited) {
                throw "Backend exited during startup. Inspect the newest files in $logRoot."
            }
        }
        if (Test-ExpectedStatus $sessionUri @(401, 200)) {
            return
        }
        Start-Sleep -Seconds 1
    }
    throw "Backend did not answer /api/v1/auth/session within 45 seconds. Inspect the newest files in $logRoot."
}

function Wait-ForFrontend {
    $frontendUri = "http://127.0.0.1:$FrontendPort/"
    for ($attempt = 1; $attempt -le 30; $attempt++) {
        if (Test-ExpectedStatus $frontendUri @(200)) {
            return
        }
        Start-Sleep -Seconds 1
    }
    throw "Frontend did not answer $frontendUri within 30 seconds. Inspect the newest files in $logRoot."
}

if (-not (Test-Path $jar)) {
    throw "Backend JAR not found at $jar. Build it first with .\mvnw.cmd -B -ntp package."
}

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'java was not found on PATH. Configure JDK 25 before starting CampusSignal.'
}

if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    throw 'node was not found on PATH. Install Node.js before starting the frontend.'
}

New-Item -ItemType Directory -Path $logRoot -Force | Out-Null
$startedBackend = $null
$startedFrontend = $null

if (-not (Test-PortListening $BackendPort)) {
    for ($launchAttempt = 1; $launchAttempt -le 3; $launchAttempt++) {
        $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
        $backendOutLog = Join-Path $logRoot "backend-$stamp-$launchAttempt.out.log"
        $backendErrorLog = Join-Path $logRoot "backend-$stamp-$launchAttempt.err.log"
        $backendCommand = @"
Set-Location '$root'
`$env:PORT='$BackendPort'
`$env:SERVER_ADDRESS='127.0.0.1'
& java -jar '$jar'
"@
        $startedBackend = Start-Process powershell.exe -PassThru -RedirectStandardOutput $backendOutLog -RedirectStandardError $backendErrorLog -ArgumentList @(
            '-NoProfile',
            '-ExecutionPolicy', 'Bypass',
            '-Command', $backendCommand
        ) -WorkingDirectory $root
        Write-Host "Starting backend on http://127.0.0.1:$BackendPort (attempt $launchAttempt/3; logs: $backendOutLog)"
        try {
            Wait-ForBackend $startedBackend
            break
        } catch {
            $startedBackend.Refresh()
            if ($startedBackend.HasExited -and $launchAttempt -lt 3) {
                Write-Warning "Backend exited before becoming healthy. Retrying after a short wait; it may be releasing an H2 database lock."
                Start-Sleep -Seconds 3
                continue
            }
            throw
        }
    }
} else {
    Write-Host "Backend already listening on port $BackendPort."
    Wait-ForBackend $null
}

if (-not (Test-PortListening $FrontendPort)) {
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $frontendOutLog = Join-Path $logRoot "frontend-$stamp.out.log"
    $frontendErrorLog = Join-Path $logRoot "frontend-$stamp.err.log"
    $frontendCommand = @"
Set-Location '$frontendRoot'
`$env:CAMPUSSIGNAL_API_TARGET='http://127.0.0.1:$BackendPort'
& .\node_modules\.bin\vite.cmd --host 127.0.0.1 --port $FrontendPort
"@
    $startedFrontend = Start-Process powershell.exe -PassThru -RedirectStandardOutput $frontendOutLog -RedirectStandardError $frontendErrorLog -ArgumentList @(
        '-NoProfile',
        '-ExecutionPolicy', 'Bypass',
        '-Command', $frontendCommand
    ) -WorkingDirectory $frontendRoot
    Write-Host "Starting frontend on http://127.0.0.1:$FrontendPort ... (logs: $frontendOutLog)"
} else {
    Write-Host "Frontend already listening on port $FrontendPort."
}

Wait-ForFrontend

Write-Host ''
Write-Host "CampusSignal is ready:"
Write-Host "  Frontend: http://127.0.0.1:$FrontendPort/"
Write-Host "  Backend:  http://127.0.0.1:$BackendPort/"
Write-Host "  Health:   /api/v1/auth/session returned the expected unauthenticated 401 (or authenticated 200)."

if (-not $NoBrowser) {
    Start-Process "http://127.0.0.1:$FrontendPort/"
}

if ($Watch) {
    Write-Host 'Monitoring both services. Press Ctrl+C to stop monitoring; it does not stop the services.'
    while ($true) {
        if (-not (Test-ExpectedStatus "http://127.0.0.1:$BackendPort/api/v1/auth/session" @(401, 200))) {
            Write-Warning "Backend health check failed. Run this script again to safely restart only the missing backend. Logs: $logRoot"
            exit 1
        }
        if (-not (Test-ExpectedStatus "http://127.0.0.1:$FrontendPort/" @(200))) {
            Write-Warning "Frontend health check failed. Run this script again to safely restart only the missing frontend. Logs: $logRoot"
            exit 1
        }
        Start-Sleep -Seconds 5
    }
}
