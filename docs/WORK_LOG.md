## Work Entry
Date: 2026-09-08
Contributor: Priyanshi Garg
Milestone: 11 September 2026 — Email Retrieval and User Preference Setup
Stage: User Profile Foundation
Branch: feature/user-profile

### Status
Completed

### Work completed
- Inspected the existing architecture, schemas, models, database initialization, and test fixtures; confirmed a clean checkout already on feature/user-profile.
- Added UserProfile using the existing SQLAlchemy Base, with a unique required email, required department/programme/year, nullable semester, JSON interests, and an automatic UTC creation timestamp.
- Registered UserProfile through app.models; the existing init_db imports that package and creates the table without changes to database initialization or Gmail code.
- Added UserProfileCreate and UserProfileRead with email validation, nonempty department/programme, positive year/semester, required string interests, whitespace trimming, lowercase normalization, ordered deduplication, blank-interest rejection, extra-field rejection, ORM reading, and UTC timestamp normalization.
- Added focused schema, registration, constraint, and persistence tests using existing database/client fixtures; verified registration independently in a fresh interpreter.
- Recorded the complete task prompt and added persistent Project Traceability Rules to AGENTS.md.

### Files changed
- app/models/user_profile.py (created)
- app/models/__init__.py (modified)
- app/schemas/user_profile.py (created)
- tests/test_user_profile.py (created)
- docs/AGENT_PROMPTS.md (created)
- docs/WORK_LOG.md (created)
- AGENTS.md (modified)

### Tests
- Baseline: python -m pytest — 35 passed, 1 warning in 0.46s, before implementation.
- New focused tests: python -m pytest tests/test_user_profile.py -v — 42 passed, 1 warning in 0.64s.
- Final complete suite: python -m pytest — 77 passed, 1 warning in 1.05s.
- Environment: the initial global-Python attempt could not run because pytest was absent. Two virtual-environment attempts each reported 20 passed and 15 setup errors because sandbox permissions blocked pytest temporary directories. All successful runs used the existing .venv Python on PATH, approved execution outside the sandbox, and PYTEST_ADDOPTS specifying a fresh UUID-suffixed --basetemp directory under .pytest_cache. No application changes were needed to resolve these environment errors.
- The single warning in each successful run is the existing Starlette/AnyIO BlockingPortal deprecation warning.

### Git
Commit: Pending
PR: Pending
PR created by: Pending

### Next stage
User Profile API/service persistence, followed by Initial Category Preference Score implementation.

---


## Work Entry
Date: 2026-09-08
Contributor: Priyanshi Garg
Milestone: 11 September 2026 — Email Retrieval and User Preference Setup
Stage: Java/Spring Boot Migration
Branch: feature/java-springboot-migration

### Status
Completed

### Reason for migration
Repository prototype was implemented in Python/FastAPI, while the submitted project proposal requires Java, Maven, and Spring Boot.

### Work completed
- Inspected the repository, Python Gmail/OAuth/retrieval behavior, models, schemas, tests, database initialization, and existing traceability documents; recorded the Python baseline before implementation.
- Created and switched from feature/user-profile to feature/java-springboot-migration.
- Added the root Maven/Spring Boot project under edu.campussignal, using Java 25 LTS and Spring Boot 3.5.16. No course/repository Java version requirement was found; Java 25 is an LTS supported by the selected Spring Boot release.
- Added Maven Wrapper 3.3.4 pinned to Maven 3.9.16 with a distribution SHA-256 checksum. Used checksum-verified portable JDK/Maven tooling and an ignored workspace dependency cache for local verification.
- Implemented local Gmail OAuth authorization, saved credential reuse/automatic refresh, paginated inbox retrieval, MIME/header/body decoding, independent persistence, and duplicate protection including concurrent inserts.
- Implemented validated profiles with ordered normalized extensible interests, relational persistence, POST and GET APIs, and safe 400/404/409 responses.
- Implemented configurable initial category scores, a score demonstration endpoint, and documented cold-start/profile-relevance assumptions. The example users score 0.6875 and 0.2875 for internships.
- Added JUnit tests with real H2 persistence and simulated Google HTTP responses; built the executable jar and checked packaged CLI wiring without calling Gmail.
- Appended the complete task prompt, preserved older traceability entries, updated AGENTS.md/README.md, and added docs/JAVA_BACKEND.md.
- Live Google browser consent and real-account retrieval were not performed. The implemented OAuth/retrieval flow is covered by automated simulation; live verification requires local credentials and the account owner's consent.

### Python status
- All existing Python implementation and tests remain intact as prototype/behavioral reference.
- git diff --numstat -- app tests requirements.txt produced no changes.
- Existing Python OAuth, Gmail retrieval/sync, watch, Pub/Sub, and history behavior was not modified or deleted.
- New backend product features belong in Java unless an explicit project decision changes the stack.

### Java implementation
- config: validated Gmail settings, configurable score weights/defaults, and strict JSON scalar handling.
- controller/dto: profile create/read/by-email endpoints, category-score response, Bean Validation, and ProblemDetail error responses.
- entity/repository: Email and UserProfile with Spring Data JPA, unique IDs/emails, UTC creation timestamps, and an ordered profile_interests element collection; H2 file storage locally and in-memory tests.
- service: provider-neutral EmailSource, email ingestion/retrieval, profile persistence/read operations, and independently testable CategoryScoreService.
- gmail: Desktop OAuth authorization, read-only scope, credential refresh/storage, Gmail list/get adapter, MIME parsing, and explicit authorize/retrieve commands.
- scoring: E is exact selected-interest membership; P uses documented year/programme/department heuristics; B defaults to 0.5 and R is a 0.0 placeholder. No learning, semantic matching, final email ranking, or October AI/NLP functionality was implemented.
- tests: profile validation/API/persistence, category bounds/differences/configuration, duplicate/concurrent email persistence, source failures, Gmail parsing/pagination, OAuth refresh, command errors, and Gmail-to-database integration.

### Files changed
- .gitignore (modified)
- AGENTS.md (modified)
- docs/AGENT_PROMPTS.md (modified)
- docs/WORK_LOG.md (modified)
- README.md (modified)
- .mvn/wrapper/maven-wrapper.properties (created)
- docs/JAVA_BACKEND.md (created)
- mvnw (created)
- mvnw.cmd (created)
- pom.xml (created)
- src/main/java/edu/campussignal/CampusSignalApplication.java (created)
- src/main/java/edu/campussignal/config/CategoryScoreProperties.java (created)
- src/main/java/edu/campussignal/config/GmailProperties.java (created)
- src/main/java/edu/campussignal/config/JsonConfiguration.java (created)
- src/main/java/edu/campussignal/controller/ApiExceptionHandler.java (created)
- src/main/java/edu/campussignal/controller/ProfileController.java (created)
- src/main/java/edu/campussignal/dto/CategoryScoreResponse.java (created)
- src/main/java/edu/campussignal/dto/IncomingEmail.java (created)
- src/main/java/edu/campussignal/dto/ProfileCreateRequest.java (created)
- src/main/java/edu/campussignal/dto/ProfileResponse.java (created)
- src/main/java/edu/campussignal/dto/RetrievalSummary.java (created)
- src/main/java/edu/campussignal/entity/Email.java (created)
- src/main/java/edu/campussignal/entity/UserProfile.java (created)
- src/main/java/edu/campussignal/gmail/GmailApiClient.java (created)
- src/main/java/edu/campussignal/gmail/GmailAuthorizationService.java (created)
- src/main/java/edu/campussignal/gmail/GmailCommand.java (created)
- src/main/java/edu/campussignal/gmail/GmailMessageMapper.java (created)
- src/main/java/edu/campussignal/repository/EmailRepository.java (created)
- src/main/java/edu/campussignal/repository/UserProfileRepository.java (created)
- src/main/java/edu/campussignal/service/CategoryScoreService.java (created)
- src/main/java/edu/campussignal/service/EmailIngestionService.java (created)
- src/main/java/edu/campussignal/service/EmailRetrievalService.java (created)
- src/main/java/edu/campussignal/service/EmailSource.java (created)
- src/main/java/edu/campussignal/service/ProfileNotFoundException.java (created)
- src/main/java/edu/campussignal/service/ProfileService.java (created)
- src/main/resources/application.properties (created)
- src/test/java/edu/campussignal/CategoryScoreConfigurationTest.java (created)
- src/test/java/edu/campussignal/CategoryScoreServiceTest.java (created)
- src/test/java/edu/campussignal/EmailPersistenceTest.java (created)
- src/test/java/edu/campussignal/ProfileApiTest.java (created)
- src/test/java/edu/campussignal/gmail/GmailApiClientTest.java (created)
- src/test/java/edu/campussignal/gmail/GmailAuthorizationServiceTest.java (created)
- src/test/java/edu/campussignal/gmail/GmailCommandTest.java (created)
- src/test/java/edu/campussignal/gmail/GmailMessageMapperTest.java (created)
- src/test/java/edu/campussignal/gmail/GmailRetrievalIntegrationTest.java (created)
- src/test/java/edu/campussignal/gmail/QueuedHttpTransport.java (created)
- src/test/resources/application-test.properties (created)

### Tests
- Python baseline: python -m pytest with the existing .venv on PATH and PYTEST_ADDOPTS pointing to a fresh .pytest_cache temporary directory — 77 passed, 1 existing Starlette/AnyIO deprecation warning in 1.20s.
- Java profile focused: Maven -Dtest=ProfileApiTest -Ddebug=false test — 26 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS.
- Java Gmail/persistence/score focused: .\\mvnw.cmd -B -ntp -Dmaven.repo.local=.m2 "-Dtest=Gmail*Test,EmailPersistenceTest,CategoryScore*Test" -Ddebug=false test — 39 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS.
- Final Maven: .\\mvnw.cmd -B -ntp -Dmaven.repo.local=.m2 -Ddebug=false clean verify — 65 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS in 19.735s. Executable target/campussignal-0.1.0-SNAPSHOT.jar produced.
- Packaged CLI smoke: launched the jar with an in-memory H2 database and --gmail.command=unsupported; the command returned the expected exit code 1 and safe usage message. No Gmail/network access occurred.
- Resolved during implementation: early compilation ran before all files were written; a Windows command-length limit required splitting file batches; the test JVM needed quotes around its Mockito agent path; the initial full suite had 2 failures (numeric interest coercion and an incompatible JSON array assertion), both fixed and verified by subsequent passing runs.
- The final build retains a Google OAuth SDK deprecation notice and a Mockito/JVM class-sharing warning; the invalid-weight startup warning and command-error messages are expected negative-test output. A conflicting transitive Commons Logging bridge was excluded.
- Previous prompt/work-log entries were verified as unchanged prefixes. git diff --check passed.

### Git
Commit: Pending
PR: Pending
PR created by: Pending

### Next stage
Perform a live Gmail authorization/retrieval demonstration with the account owner's local OAuth configuration, confirm persisted emails and duplicate counts on a second run, and review/commit the Java September milestone. Keep Python as reference; October AI/NLP work requires a separate explicit task.

---
