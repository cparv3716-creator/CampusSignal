# CampusSignal

CampusSignal is a Java 25, Maven, and Spring Boot backend for turning university email into structured, actionable, and personalized information. The active implementation combines Gmail ingestion with deterministic classification, action tracking, deadline presentation, and a ranked profile feed.

Detailed behavior and operational notes are in [docs/JAVA_BACKEND.md](docs/JAVA_BACKEND.md).

## Current capabilities

- Gmail desktop OAuth with a local token directory
- One-shot Gmail retrieval with MIME and HTML-to-text handling
- Gmail `users.watch`, Google Cloud Pub/Sub pull listening, and Gmail history synchronization
- Stale-history recovery through bounded mailbox retrieval
- H2/JPA email persistence with stable-ID duplicate protection
- Deterministic email classification with persisted confidence
- Explicit classification backfill for older incomplete rows
- Read-only email list and detail APIs
- Validated user profile creation and lookup
- Configurable `CategoryScoreService` relevance scoring
- Deterministic action candidate, deadline, and safe HTTP/HTTPS URL extraction
- Persisted action tracking with guarded status transitions
- Derived effective status, urgency, and deadline countdown
- Personalized ranked actionable feed with deterministic explanations
- React + Vite dashboard with Inbox review, deterministic Quick summary, profile editing, and action tracking
- Isolated synthetic Phase 4 demo fixture for safe collaboration
- Local-only Gmail sync bootstrap flow, disabled unless explicitly configured
- Disabled-by-default local Ollama summarization service with deterministic fallback
- JUnit/Spring Boot tests, JaCoCo coverage reports, SpotBugs, and PMD

## Project layout

```text
pom.xml
mvnw / mvnw.cmd
.mvn/wrapper/
config/pmd/
src/main/java/edu/campussignal/
  command/       classification maintenance commands
  config/        application configuration
  controller/    HTTP endpoints and exception handling
  dto/           validated API and service contracts
  entity/        JPA entities
  gmail/         Gmail OAuth and API adapter
  pubsub/        Google Cloud Pub/Sub adapter
  repository/    Spring Data JPA repositories
  service/       ingestion, classification, actions, profiles, and ranking
src/main/resources/application.properties
src/test/java/edu/campussignal/
src/test/resources/application-test.properties
```

Provider-specific Gmail and Pub/Sub code stays behind application services. Core retrieval consumes the `EmailSource` contract, keeping ingestion independent of the provider adapter.

## Prerequisites

- JDK 25 with `JAVA_HOME` configured
- Network access for the Maven Wrapper's first dependency download
- A Google Cloud desktop OAuth client only when using Gmail commands
- A Google Cloud Pub/Sub topic and pull subscription only when using realtime synchronization

Copy `.env.example` values into your local environment as needed. Never commit credentials, generated OAuth tokens, or local database files.

## Build, test, and analyze

From PowerShell in the repository root:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean package
.\mvnw.cmd spotbugs:check
.\mvnw.cmd pmd:check
```

JaCoCo generates coverage output under `target/site/jacoco/` during the test lifecycle. On Unix-like systems, use `./mvnw` in place of `.\mvnw.cmd`.

## Run the HTTP API

```powershell
.\mvnw.cmd spring-boot:run
```

The default address is `http://127.0.0.1:8080`. The application uses file-backed H2 at `./data/campussignal` by default; tests use isolated in-memory H2 databases.

## Frontend demo

The optional React + Vite demo client lives in [`frontend/`](frontend/README.md). Start the Spring Boot API first, then run `npm install` and `npm run dev` from that directory. Its Vite development proxy sends `/api` requests to the local backend, so the browser client does not hardcode a backend URL.

### Recommended collaborative demo

Use the synthetic fixture rather than a Gmail-backed database when sharing the project
with teammates. It creates fictional messages and a fictional profile in an isolated
H2 database, and it never uses OAuth credentials or real mailbox content. Start it with
the command in [Synthetic Phase 4 demo fixture](#synthetic-phase-4-demo-fixture), then
point Vite at `http://127.0.0.1:8091`:

```powershell
$env:CAMPUSSIGNAL_API_TARGET = "http://127.0.0.1:8091"
node .\node_modules\vite\bin\vite.js --host 127.0.0.1 --port 5174
```

The shared branch must not contain `data/`, `credentials.json`, `.gmail-tokens/`,
`.env` files, generated logs, frontend `node_modules/`, or frontend `dist/` output.
The Gmail sync flow and local Ollama integration are experimental local-development
features; neither should be enabled for a shared demo.

### Local dashboard Gmail sync

This is an experimental, localhost-only development feature, not the recommended
collaborative demo path. It must use a copied database and credentials stored outside
the repository. Never point it at the original mailbox database or publish its
configuration.

The browser never receives the Gmail operator secret. Start the copied real-mail backend
with a strong environment variable and loopback binding:

```powershell
$env:CAMPUSSIGNAL_OPERATOR_SECRET = "<strong-local-secret-at-least-32-characters>"
$env:SERVER_ADDRESS = "127.0.0.1"
$env:PORT = "8092"
$env:DATABASE_URL = "jdbc:h2:file:./data/<COPIED_H2_DATABASE_NAME>"
.\mvnw.cmd spring-boot:run
```

In another PowerShell window, issue a one-time browser session bootstrap:

```powershell
$env:CAMPUSSIGNAL_OPERATOR_SECRET = "<same-local-secret>"
.\scripts\campussignal-gmail-bootstrap.ps1
```

The helper sends the secret only over loopback, opens a one-time fragment URL, and the
dashboard exchanges it for an HttpOnly session cookie plus an in-memory CSRF token. The
server enforces loopback binding, loopback clients, allowed origins, a 10-message maximum,
and one sync at a time. Never expose this endpoint beyond localhost.

## Synthetic Phase 4 demo fixture

The synthetic fixture is disabled by default and never uses Gmail, OAuth, or real student data.
It refuses to run unless the `demo` profile, an explicit confirmation, and an isolated
`campussignal-demo` H2 database path are all provided. It uses the normal ingestion,
classification, action extraction, action creation, and feed services.

Run it in a separate PowerShell window:

```powershell
.\mvnw.cmd spring-boot:run `
  "-Dspring-boot.run.profiles=demo" `
  "-Dspring-boot.run.arguments=--campussignal.demo.fixture=phase4,--campussignal.demo.confirm=synthetic-only,--spring.datasource.url=jdbc:h2:file:./data/campussignal-demo"
```

The command is idempotent and creates six fictional emails, pending actions where the
existing extractor finds one, and the synthetic profile
`demo.student@demo.example.com`. Do not point this command at the normal
`./data/campussignal` database.

## Vercel container and PostgreSQL

Vercel's container runtime detects the root `Dockerfile.vercel`. The image builds with
Maven 3.9.16 and Java 25, runs as an unprivileged user, activates the `prod` profile,
binds to `0.0.0.0`, and listens on Vercel's `PORT` value (port 80 by default).

Configure these runtime variables in the Vercel project; do not commit their values:

- `JDBC_DATABASE_URL`: a full JDBC URL such as `jdbc:postgresql://host:5432/database`
- `DB_USERNAME` and `DB_PASSWORD`
- optional pool limits: `DB_MAX_POOL_SIZE`, `DB_MIN_IDLE`, and `DB_CONNECTION_TIMEOUT_MS`
- optional `JPA_DDL_AUTO` (defaults to `update` for the initial academic deployment)

The container is stateless. Production data must live in PostgreSQL; the local default
continues to use H2. Schema migrations are not yet versioned, so change `JPA_DDL_AUTO`
to `validate` only after an external migration process has created the schema.

For a synthetic-only deployment, set `DEMO_DATA_ENABLED=true`. Startup idempotently
creates a fictional profile (`student@demo.example.com`), two fictional messages, and
two actions through the normal ingestion and action services. It does not load Gmail
credentials or contact Google. Leave the setting false for an empty or real-data database.

Frontend clients should use relative `/api/v1/...` URLs and be routed through the same
origin as the backend. CampusSignal intentionally does not enable permissive cross-origin
access. The teammate frontend currently lives only on its collaboration branch and is not
merged here; reconcile its additional endpoint contract separately rather than copying or
overwriting that branch.

The existing read and action APIs do not yet enforce end-user authentication or per-user
data ownership. A public deployment is therefore suitable only for fictional demo data or
behind an external access-control layer. Do not connect real Gmail data to a publicly
reachable instance until application-level authentication and authorization are implemented.

## CS5013 final demonstration image

The root `Dockerfile` is the self-contained submission image. It activates only the
`demo` profile, listens on container port 80, uses an in-memory H2 database, and seeds
two fictional `demo.example.com` emails. Gmail sync and local AI are disabled, and the
image needs no database, OAuth configuration, external service, secret, or volume.
`Dockerfile.vercel` and the existing local and `prod` profiles are unchanged.

The submission image keeps multi-user authentication and owner isolation enabled. Its
demo-only login creates a session for the fictional demo student without contacting
Google. The image can be evaluated from PowerShell in four commands:

```powershell
docker load --input .\campussignal-final-integrated.tar
docker run --detach --rm --name campussignal-final --publish 8080:80 campussignal:final
Invoke-WebRequest -Method Post http://localhost:8080/api/v1/auth/demo -SessionVariable demoSession
Invoke-RestMethod http://localhost:8080/api/v1/me/emails -WebSession $demoSession
```

After evaluation, remove the disposable container with
`docker stop campussignal-final`.

## Gmail authorization and synchronization

Enable the Gmail API, create a desktop OAuth client, and keep the downloaded credentials outside source control. For realtime synchronization, also enable Pub/Sub, configure a topic and pull subscription, and grant the Gmail push service account permission to publish to the topic.

After building, the supported command modes are:

```powershell
java -jar target/campussignal-0.1.0-SNAPSHOT.jar --spring.main.web-application-type=none --gmail.command=authorize
java -jar target/campussignal-0.1.0-SNAPSHOT.jar --spring.main.web-application-type=none --gmail.command=retrieve --gmail.max-results=10
java -jar target/campussignal-0.1.0-SNAPSHOT.jar --spring.main.web-application-type=none --gmail.command=watch
java -jar target/campussignal-0.1.0-SNAPSHOT.jar --spring.main.web-application-type=none --gmail.command=listen
```

`authorize` performs the browser-based consent flow. `retrieve` fetches current inbox messages. `watch` registers Gmail push notifications and stores the returned history state. `listen` consumes the configured pull subscription, synchronizes new messages through Gmail history, ignores already-processed notifications, and performs bounded recovery when Gmail reports a stale history cursor.

The optional HTTP sync endpoint is strictly for an explicitly enabled local workflow. Set
`GMAIL_SYNC_ENABLED=true` and a strong `GMAIL_SYNC_TOKEN`, then call
`POST /api/v1/emails/sync` from loopback with the token in
`X-CampusSignal-Sync-Token`. It returns counts only. The endpoint is absent by default and
is forcibly disabled by the production profile; do not use it to expose Gmail through a
public deployment.

## Classification and backfill

Newly ingested email is classified deterministically and stores both an `EmailCategory` and a `ClassificationConfidence`. To classify older rows whose category or confidence is missing, stop other CampusSignal processes, back up the local database, and run:

```powershell
java -jar target/campussignal-0.1.0-SNAPSHOT.jar --spring.main.web-application-type=none --classification.backfill=true
```

Backfill is explicit, transactional, and idempotent. It does not run during normal startup or API reads.

## HTTP API

| Endpoint | Purpose |
|---|---|
| `GET /api/v1/emails` | List persisted email metadata and classification |
| `GET /api/v1/emails/{id}` | Read one persisted email |
| `GET /api/v1/emails/{emailId}/action-candidate` | Preview deterministic action metadata |
| `POST /api/v1/profiles` | Create a validated user profile |
| `GET /api/v1/profiles/{id}` | Read a profile by ID |
| `GET /api/v1/profiles/by-email/{email}` | Read a profile by normalized email |
| `GET /api/v1/profiles/{id}/category-score?category=...` | Explain a category preference score |
| `GET /api/v1/profiles/{id}/email-feed` | Read-only personalized ranked email feed; no action creation required |
| `POST /api/v1/emails/{emailId}/actions` | Create a tracked action |
| `GET /api/v1/actions` | List tracked actions |
| `GET /api/v1/actions/{actionId}` | Read one tracked action |
| `GET /api/v1/emails/{emailId}/actions` | List actions for an email |
| `PATCH /api/v1/actions/{actionId}/status` | Complete or dismiss a pending action |
| `GET /api/v1/profiles/{profileId}/action-feed` | Return the personalized ranked actionable feed |
| `POST /api/v1/gmail/sync` | Explicitly retrieve up to 10 recent Inbox messages after local operator bootstrap |

Action responses derive effective status, urgency, and `daysRemaining` from the current UTC date without mutating stored state. The profile feed combines category preference, classification confidence, and urgency using stable tie-breaking and exposes a deterministic ranking explanation.

## Configuration

`src/main/resources/application.properties` defines every supported environment override. The main groups are:

- server binding: `SERVER_ADDRESS`, `PORT`
- H2 datasource: `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- Gmail OAuth and retrieval: `GMAIL_CREDENTIALS_FILE`, `GMAIL_TOKEN_DIRECTORY`, `GMAIL_CALLBACK_PORT`, `GMAIL_MAX_RESULTS`, `GMAIL_USER_ID`
- realtime synchronization: `GOOGLE_CLOUD_PROJECT`, `GMAIL_PUBSUB_TOPIC`, `GMAIL_PUBSUB_SUBSCRIPTION`, `GMAIL_RECOVERY_MAX_RESULTS`
- local-only HTTP sync: `GMAIL_SYNC_ENABLED`, `GMAIL_SYNC_TOKEN`
- synthetic fixtures: `DEMO_DATA_ENABLED`
- category scoring: the six `CATEGORY_SCORE_*` values shown in `.env.example`

## Evaluation evidence

Java test, debugging, coverage, and static-analysis evidence is recorded under `docs/`. Historical traceability files preserve the prompts and results that produced the current implementation; the code evaluated from this branch is the Java source and tests under `src/`.
