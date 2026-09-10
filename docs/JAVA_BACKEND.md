# CampusSignal Java backend

Java + Maven + Spring Boot is the course-compliant CS5013 implementation going forward.
The existing `app/`, `tests/`, and `requirements.txt` remain the earlier Python/FastAPI
prototype and behavioral reference. Do not add new Python product features unless explicitly
approved. No Python code, database, credentials, or token files are migrated or deleted by this work.

## Toolchain and layout

No repository/course configuration specified a Java version. This implementation selects
**Java 25 LTS**, **Spring Boot 3.5.16**, and **Maven 3.9.16**. Java 25 is the current LTS;
Spring Boot 3.5.16 explicitly supports Java 25. The established Spring Boot 3.5 line provides
the required Spring MVC, JPA, and validation support without extra framework modules.
The standard Maven Wrapper (3.3.4, script-only distribution) pins Maven 3.9.16.

Sources: [Java LTS roadmap](https://www.oracle.com/uk/java/technologies/java-se-support-roadmap.html),
[Spring Boot system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html).

```text
pom.xml
mvnw / mvnw.cmd
.mvn/wrapper/maven-wrapper.properties
src/main/java/edu/campussignal/
  CampusSignalApplication.java
  config/       # Gmail settings, score settings, strict JSON input
  controller/   # Profile API and safe HTTP errors
  dto/          # Validated requests, responses, normalized email contract
  entity/       # Email and UserProfile
  repository/   # Spring Data JPA
  service/      # Profiles, ingestion, retrieval, category scores
  gmail/        # OAuth, Google API adapter, MIME parsing, local command
src/main/resources/application.properties
src/test/java/edu/campussignal/
src/test/resources/application-test.properties
```

Dependencies cover Spring Web, Spring Data JPA, Bean Validation, Google Gmail/OAuth clients,
H2, MIME decoding (Jakarta Mail), HTML-to-text parsing (jsoup), and Spring Boot Test/JUnit.
There are no AI/LLM dependencies. MIME/HTML parsing only retrieves the email body; it does not
extract events, deadlines, eligibility, categories, or summaries.

## Build and run

Install a JDK 25 and set `JAVA_HOME` to its installation directory. The wrapper downloads Maven
on first use; Maven Central access is needed to resolve dependencies.

From the repository root in PowerShell:

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

On Unix, use `sh mvnw` in place of `.\mvnw.cmd`, or use `mvn` with Maven 3.9.16 installed.
The API binds to `127.0.0.1:8080` by default. `SERVER_ADDRESS` and `PORT` override the binding.
Starting the API requires no Gmail credentials and does not open an OAuth browser.

The agent's verification used a checksum-verified portable Temurin 25.0.4.1+1 JDK and Maven
under ignored `.tools/`, with `-Dmaven.repo.local=.m2` for an ignored workspace dependency
cache. These tool downloads are local artifacts, not project sources or prerequisites tied
to a contributor-specific absolute path.

## Gmail authorization and retrieval

This milestone supports one authorized local Gmail mailbox per Java database/token directory.
It does not port Python watch, Pub/Sub, Gmail history synchronization, or real-time monitoring.

1. Enable the Gmail API in your Google Cloud project.
2. Configure the OAuth consent screen and allow your account as a test user when applicable.
3. Create a **Desktop app** OAuth client and save its downloaded JSON outside the source tree
   (or as the ignored root `credentials.json`).
4. Set `GMAIL_CREDENTIALS_FILE` to that file. Optionally set `GMAIL_TOKEN_DIRECTORY` to a private
   directory; its default is the ignored `.gmail-tokens/`. Java uses its own credential store,
   separate from the Python `token.json` format.
5. Build the executable jar and run the explicit authorization command on your local machine:

```powershell
$env:GMAIL_CREDENTIALS_FILE = 'C:\path\outside-repository\credentials.json'
.\mvnw.cmd package
java -jar target/campussignal-0.1.0-SNAPSHOT.jar --spring.main.web-application-type=none --gmail.command=authorize
java -jar target/campussignal-0.1.0-SNAPSHOT.jar --spring.main.web-application-type=none --gmail.command=retrieve --gmail.max-results=10
```

The authorization command uses Google's installed-app flow, a browser consent step, and a
loopback callback at `127.0.0.1:8888`. Set `GMAIL_CALLBACK_PORT` if that port is unavailable.
Only `https://www.googleapis.com/auth/gmail.readonly` is requested. OAuth secrets come from the
configured local file; refresh credentials are stored locally and expired access tokens are
refreshed by the Google client. Retrieval requires prior authorization and never initiates
interactive consent by itself. To change accounts, use a separate token directory and database.

Retrieval requests INBOX message IDs using `users.messages.list`, follows pagination up to the
configured limit (1–500), then requests `format=full` for each message. It decodes MIME subject
headers, sender addresses, Base64URL bodies, declared charsets, and nested multipart text.
Plain text takes priority over HTML, which is converted to text with script/style content removed.
Attachments are not ingested. A valid Date header is preferred, with Gmail's `internalDate`
(epoch milliseconds) as fallback, matching the prototype's timestamp behavior. Missing optional
subject/sender headers use `(no subject)`/`unknown`; no usable timestamp or ID counts as a failed message.

The command prints counts only: `fetched`, `created`, `alreadyExisted`, and `failed`.
An individual failed message does not discard other successfully persisted messages.
Listing/authorization failures and partial retrieval failures return exit code 1; complete success
returns 0. Provider/SQL exception payloads are not printed. Credentials, tokens, email bodies,
and private keys must never be committed or copied into documentation.

Setup references: [Google's Java Gmail quickstart](https://developers.google.com/workspace/gmail/api/quickstart/java),
[Google Java OAuth support](https://developers.google.com/api-client-library/java/google-api-java-client/oauth2),
[Gmail message representation](https://developers.google.com/workspace/gmail/api/reference/rest/v1/users.messages).

## Relational storage

Local development uses file-backed H2 at `data/campussignal.mv.db`; test contexts use isolated
in-memory H2. `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` configure the datasource.
Only the H2 driver is included at this stage. Hibernate's `ddl-auto=update` initializes the local
schema; this is a local milestone setup, not a production migration strategy.

- `emails`: generated ID, unique required `gmail_message_id`, subject, sender, UTC
  `received_at`, full text body, and automatic UTC `created_at`.
- `user_profiles`: generated ID, unique required email, department, programme, positive
  `study_year`, optional positive semester, and automatic UTC creation time.
- `profile_interests`: a JPA element collection with `profile_id`, `interest_order`, and
  string interest. Ordering survives a database round trip.

Core retrieval depends on the `EmailSource` interface and an `IncomingEmail` record, not Google
SDK types. Duplicate detection first checks the stable message ID. A unique database constraint
also protects concurrent inserts; the failed transaction rolls back before an existing record
is retrieved. The original stored email is retained on duplicate retrieval.

## Profile API

All HTTP responses use DTOs, not JPA entities. Email addresses are stripped and lowercased for
consistent uniqueness and lookup. Department/programme are stripped and must be nonblank.
Year is required and at least 1; semester is optional and at least 1 when supplied.
Interests must be a nonempty list of nonblank strings; normalization strips surrounding
whitespace, lowercases with Locale.ROOT, and removes duplicates in first-occurrence order.
Interests are extensible; there is no restrictive enum. Unexpected fields, numeric/boolean
interests, fractional years, and invalid scalar types are rejected.

```powershell
$profile = @{
    email = 'student@example.edu'
    department = 'Computer Science'
    programme = 'BTech'
    year = 3
    interests = @(' Internships ', 'RESEARCH', 'internships', 'hackathons')
} | ConvertTo-Json
$created = Invoke-RestMethod http://127.0.0.1:8080/api/v1/profiles -Method Post -ContentType 'application/json' -Body $profile
Invoke-RestMethod "http://127.0.0.1:8080/api/v1/profiles/$($created.id)"
Invoke-RestMethod 'http://127.0.0.1:8080/api/v1/profiles/by-email/student@example.edu'
Invoke-RestMethod "http://127.0.0.1:8080/api/v1/profiles/$($created.id)/category-score?category=internships"
```

| Endpoint | Successful result |
|---|---|
| POST /api/v1/profiles | 201 with Location header and profile |
| GET /api/v1/profiles/{id} | 200 with profile |
| GET /api/v1/profiles/by-email/{email} | 200 with profile |
| GET /api/v1/profiles/{id}/category-score?category=internships | 200 with category, score, and components |

Invalid input returns 400, missing profiles return 404, and duplicate email returns 409.
Errors use ProblemDetail with safe descriptions. Profile endpoints have no authentication,
as requested for this local milestone.

## Initial category preference score

`CategoryScoreService` computes:

```text
CategoryScore(u,c) = 0.40E + 0.15P + 0.35B + 0.10R
```

All components are finite values in [0,1]. Weights are configurable, must be nonnegative and
sum to 1; invalid settings fail startup rather than silently changing the proposal formula.

| Component | September behavior |
|---|---|
| E: explicit preference | 1 when the normalized category exactly matches a selected interest, otherwise 0 |
| P: profile relevance | Transparent initial heuristics below; no eligibility or content classification |
| B: behavioral preference | Configured neutral default 0.5 when history is absent; an independent service overload can accept a validated value, but no behavior collection or learning exists |
| R: related-interest similarity | Constant configurable placeholder 0.0 for all users/categories; no semantic or related-interest computation exists |

P uses these deterministic **implementation assumptions**, not empirically learned rules or
additional requirements claimed to come from the proposal:

- Internships: min(year / 4, 1), giving later-year students a higher initial profile component.
- Research: 1 for programme names containing phd, master, mtech, or msc; otherwise
  0.6 × min(year / 4, 1).
- Hackathons: 1 for departments containing computer or engineering, or programmes containing
  btech; otherwise 0.3.
- Any other category: neutral 0.5. New categories remain valid without an enum.

The initial four-year scale is a simple local heuristic; it is not a claim about the duration
of every programme. Department and programme comparisons are case-insensitive.

| Environment variable | Default |
|---|---|
| CATEGORY_SCORE_EXPLICIT_WEIGHT | 0.40 |
| CATEGORY_SCORE_PROFILE_WEIGHT | 0.15 |
| CATEGORY_SCORE_BEHAVIORAL_WEIGHT | 0.35 |
| CATEGORY_SCORE_RELATED_WEIGHT | 0.10 |
| CATEGORY_SCORE_INITIAL_BEHAVIORAL | 0.5 |
| CATEGORY_SCORE_INITIAL_RELATED | 0.0 |

For two year-3 users with identical programme/department, User A selecting internships, research,
and hackathons receives **0.6875** for internships. User B selecting sports, cultural events, and
clubs receives **0.2875**. Their difference is exactly the 0.40 explicit-preference contribution.

Example User A response:

```json
{
  "category": "internships",
  "score": 0.6875,
  "components": {
    "explicitPreference": 1.0,
    "profileRelevance": 0.75,
    "behavioralPreference": 0.5,
    "relatedInterestSimilarity": 0.0
  }
}
```

These scores describe category preferences only. There is no final user-email ranking,
classification, summarization, extraction, embedding, semantic similarity, behavior-learning
feedback, personalized feed, critical-message override, or frontend.

## Verification and remaining account setup

JUnit exercises the real JPA schema and repositories, duplicate constraints and concurrent
ingestion, profile validation/API, configurable scores, MIME decoding, Gmail pagination,
stored OAuth reuse and automatic token refresh, command errors/exit codes, and the complete
simulated Gmail HTTP-response-to-database path. Tests use synthetic values and an in-memory HTTP
transport; they do not read real credentials, open a consent browser, or call a Gmail account.

A live Google consent/retrieval demonstration still requires the account owner's local
OAuth configuration and consent. Automated success does not claim that this live step occurred.
See [WORK_LOG.md](WORK_LOG.md) for actual test results and [AGENT_PROMPTS.md](AGENT_PROMPTS.md)
for the preserved task prompts.
