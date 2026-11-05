param(
    [string]$Backend = "http://127.0.0.1:8092",
    [string]$Frontend = "http://127.0.0.1:5174/"
)

function Test-LoopbackHttpUrl([string]$Value) {
    try {
        $uri = [Uri]$Value
        if ($uri.Scheme -ne "http" -or $uri.UserInfo -or $uri.Query -or $uri.Fragment) {
            return $false
        }
        if ($uri.Host -eq "localhost") {
            return $true
        }
        $address = [System.Net.IPAddress]::Parse($uri.Host)
        return [System.Net.IPAddress]::IsLoopback($address)
    } catch {
        return $false
    }
}

if (-not (Test-LoopbackHttpUrl $Backend) -or -not (Test-LoopbackHttpUrl $Frontend)) {
    throw "Backend and Frontend must be plain HTTP loopback URLs."
}

$operatorSecret = $env:CAMPUSSIGNAL_OPERATOR_SECRET
if ([string]::IsNullOrWhiteSpace($operatorSecret) -or $operatorSecret.Length -lt 32) {
    throw "Set CAMPUSSIGNAL_OPERATOR_SECRET to a strong value of at least 32 characters."
}

$headers = @{ "X-CampusSignal-Operator-Secret" = $operatorSecret }
$code = Invoke-RestMethod -Method Post -Uri "$Backend/api/v1/gmail/operator/bootstrap" `
    -Headers $headers -ContentType "application/json"
Start-Process "$Frontend#campussignal-operator-code=$code"
