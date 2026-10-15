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
| `POST /api/v1/emails/{emailId}/actions` | Create a tracked action |
| `GET /api/v1/actions` | List tracked actions |
| `GET /api/v1/actions/{actionId}` | Read one tracked action |
| `GET /api/v1/emails/{emailId}/actions` | List actions for an email |
| `PATCH /api/v1/actions/{actionId}/status` | Complete or dismiss a pending action |
| `GET /api/v1/profiles/{profileId}/action-feed` | Return the personalized ranked actionable feed |

Action responses derive effective status, urgency, and `daysRemaining` from the current UTC date without mutating stored state. The profile feed combines category preference, classification confidence, and urgency using stable tie-breaking and exposes a deterministic ranking explanation.

## Configuration

`src/main/resources/application.properties` defines every supported environment override. The main groups are:

- server binding: `SERVER_ADDRESS`, `PORT`
- H2 datasource: `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- Gmail OAuth and retrieval: `GMAIL_CREDENTIALS_FILE`, `GMAIL_TOKEN_DIRECTORY`, `GMAIL_CALLBACK_PORT`, `GMAIL_MAX_RESULTS`, `GMAIL_USER_ID`
- realtime synchronization: `GOOGLE_CLOUD_PROJECT`, `GMAIL_PUBSUB_TOPIC`, `GMAIL_PUBSUB_SUBSCRIPTION`, `GMAIL_RECOVERY_MAX_RESULTS`
- category scoring: the six `CATEGORY_SCORE_*` values shown in `.env.example`

## Evaluation evidence

Java test, debugging, coverage, and static-analysis evidence is recorded under `docs/`. Historical traceability files preserve the prompts and results that produced the current implementation; the code evaluated from this branch is the Java source and tests under `src/`.
