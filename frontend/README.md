# CampusSignal frontend

This Vite + React interface is a demo client for the Phase 4 Spring Boot API. It creates and looks up profiles, displays the live personalized action feed, previews deterministic action candidates, and updates an existing action to `COMPLETED` or `NOT_INTERESTED`.

## Run locally

Start the Phase 4 backend first from the repository root:

```powershell
.\mvnw.cmd spring-boot:run
```

Then, in this directory:

```powershell
npm install
npm run dev
```

Open the Vite URL shown in the terminal. The Vite development proxy forwards browser requests beginning with `/api` to `http://127.0.0.1:8080` by default; React code never contains a backend base URL.

To run the frontend against the isolated synthetic demo backend on port 8091, set the
proxy target before starting Vite:

```powershell
$env:CAMPUSSIGNAL_API_TARGET = "http://127.0.0.1:8091"
npm run dev
```

The variable only configures Vite's local development proxy. It does not add demo data
to the React application or change any request paths. In a new PowerShell session, the
default port 8080 target is used again. To clear it in the current session explicitly:

```powershell
Remove-Item Env:CAMPUSSIGNAL_API_TARGET -ErrorAction SilentlyContinue
```

For a production build check:

```powershell
npm run build
```

## API behavior

The interface uses only implemented endpoints:

- `POST /api/v1/profiles` and profile lookup endpoints
- `GET /api/v1/emails` and `GET /api/v1/actions` for the persisted Inbox review
- `GET /api/v1/emails/{emailId}/action-candidate` for read-only candidate previews
- `POST /api/v1/emails/{emailId}/actions` only after explicit user confirmation
- `GET /api/v1/profiles/{profileId}/action-feed`
- `GET /api/v1/profiles/{profileId}/email-feed` for automatic personalized email ranking
- `PATCH /api/v1/actions/{actionId}/status`
- `POST /api/v1/gmail/sync` after the local operator bootstrap helper establishes a session

The Inbox recognizes any existing action for an email, including terminal actions, and
does not offer a duplicate or reopen control. The candidate endpoint is a read-only
preview: the client does not create actions from it or display placeholder action data.
The Overview feed ranks stored emails automatically from the selected profile; it does
not require action preview or tracked action creation. An empty `204 No Content` result is shown as no detected candidate. Gmail OAuth remains
local CLI-only. For the localhost-only sync button, set `CAMPUSSIGNAL_OPERATOR_SECRET`
in the backend and run `..\scripts\campussignal-gmail-bootstrap.ps1`; the secret is
never embedded in the frontend or stored in browser storage.
