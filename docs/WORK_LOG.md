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

## Work Entry
Date: 2026-10-06
Contributor: Codex
Milestone: Email Classification Discovery and Validation
Stage: Analysis and Specification Only
Branch: phase4-live-demo

### Status
Completed — awaiting human approval before any classifier or JUnit implementation.

### Starting repository state
- Starting commit: `8264fe1b4980642ae9f9d4db0d27fda32a160e1b`.
- Working tree was clean at task start.
- No branch switch, commit, push, merge, or PR was performed.

### Mailbox analysis
- Used the student's IIT Madras Smail connection through Gmail read-only search/read operations only.
- Screened recent mail and excluded authentication/security messages, OTPs, personal conversations, receipts/delivery notices, unrelated commercial/platform mail, and obvious spam.
- Discovery set: 50 meaningful university messages.
- Validation set: 20 held-out meaningful university messages.
- Sample window: 2026-10-01 through 2026-10-06.
- Did not open or read attachments.
- Did not send, delete, label, archive, forward, mark, or otherwise modify mail.
- Did not store message IDs, raw bodies, account identifiers, credentials, tokens, phone numbers, addresses, or private mailbox content in Git documentation.

### Taxonomy proposed
- Frozen discovery taxonomy and counts: `ACADEMIC_RESEARCH` 9, `CAREER` 9, `EVENT_ACTIVITY` 11, `CALL_FOR_PARTICIPATION` 7, `ADMINISTRATIVE_NOTICE` 7, `WELLBEING_SUPPORT` 3, and `GENERAL_OTHER` 4.
- Validation-driven proposal, shown separately rather than silently changing the frozen taxonomy: add `RECOGNITION_ACHIEVEMENT`.
- Kept a required `GENERAL_OTHER` fallback and recommended purpose-based conflict handling, evidence-zone filtering, negation handling, distinct-feature counting, and High/Medium/Low confidence classes.

### Validation summary
- Total: 20.
- Clean classifications under the frozen taxonomy: 12 (60%).
- Ambiguous: 2 (10%): academic-course registration versus participation call, and facility feedback versus participation call.
- Definite wrong specialist assignments: 0 when `GENERAL_OTHER` is treated as a valid fallback.
- Missing-category/under-specific results: 6 (30%), all completed award/achievement announcements.
- Frozen result counts: `GENERAL_OTHER` 7, `CALL_FOR_PARTICIPATION` 6, `ACADEMIC_RESEARCH` 2, `ADMINISTRATIVE_NOTICE` 2, `EVENT_ACTIVITY` 2, `WELLBEING_SUPPORT` 1, `CAREER` 0.
- Retrospective result if the proposed recognition category were approved: 18 clean and 2 ambiguous; this is a manual estimate, not implemented classifier accuracy.

### Decisions made for this specification
- Accepted for the draft only: compact taxonomy, reachable fallback, separate evidence zones, no raw keyword-frequency boosting, and explicit before/after validation reasoning.
- Rejected: keyword-only, sender-only, forced-specialist, and raw-occurrence classification.
- Deferred: narrow career/event subtypes, unrelated survey/commerce subtypes, attachment inspection, AI/NLP, embeddings, ranking, and behavioural learning.

### Decisions left for human approval
- Final categories and whether to add `RECOGNITION_ACHIEVEMENT`.
- Any merges/removals, especially whether low-frequency `WELLBEING_SUPPORT` stays separate.
- Priority order, subject/body weights, confidence thresholds, quote rescue behavior, signature/footer/URL stripping, tie behavior, and fallback behavior.
- Treatment of wellness activities, facility surveys, academic registration, and multi-category messages.
- Exact evidence phrases, negation window, Unicode/word-boundary rules, and whether the API exposes ambiguity/runner-up information.

### Files changed
- `docs/EMAIL_CLASSIFICATION_DISCOVERY.md` (created)
- `docs/TEST_LOG.md` (created)
- `docs/BUG_LOG.md` (created)
- `docs/AGENT_PROMPTS.md` (appended concise prompt summary)
- `docs/WORK_LOG.md` (appended this entry)

### Implementation status
- No Java application source was modified.
- No existing JUnit test was modified.
- No classifier or proposed test was implemented.
- No Gmail behavior, database schema, profile logic, score/ranking logic, or Python reference code was modified.
- No implementation bugs were fabricated or recorded during this discovery-only stage.

### Tests and checks
- Privacy scan: `rg` scan of the new artifacts for mailbox-ID, private-account, institutional-address, and phone-number patterns (sensitive search terms intentionally omitted from this log) — no matches.
- Diff whitespace check: `git diff --check` — passed.
- Initial Maven attempt: `.\\mvnw.cmd -B -ntp -Dmaven.repo.local=.m2 -Ddebug=false test` — failed before compilation because PowerShell parsed the unquoted `-D` property as an invalid lifecycle phase.
- Corrected full suite: `.\\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -Ddebug=false test` — BUILD SUCCESS; 77 tests run, 0 failures, 0 errors, 0 skipped.
- Existing expected negative-test log messages and the existing Gmail OAuth deprecation/JVM class-sharing warnings remained; no test failure resulted.

### Failures and limitations
- The first Maven invocation was a command-quoting error, not a product/test failure; the corrected command passed.
- One mailbox, one six-day window, purposive sampling, message-level rather than thread-level units, no attachment content, and manual rather than executable validation limit generalisability.
- Validation headers/snippets were visible for relevance screening, but validation bodies and classification evidence remained held out until after taxonomy freeze.

### Git
Commit: Pending
PR: Pending
PR created by: Pending

### Next action
Obtain human approval for the category set, post-validation recognition addition, precedence, subject/body and quote handling, confidence classes, evidence-zone filtering, ambiguity behavior, and fallback. After approval, implement the approved deterministic Java classifier and the highest-priority synthetic JUnit cases in a separate task.

---

## Work Entry
Date: 2026-10-06
Contributor: Codex
Milestone: Deterministic Email Classification
Stage: Classification Specification V1 Approval
Branch: phase4-live-demo

### Status
Completed — documentation/specification only.

### Source
- Discovery and validation evidence: `docs/EMAIL_CLASSIFICATION_DISCOVERY.md`.
- Human-approved decisions supplied after review of the mailbox discovery artifact.
- Starting repository commit: `8264fe1b4980642ae9f9d4db0d27fda32a160e1b`.

### Approved categories
- `ACADEMIC_RESEARCH`
- `CAREER`
- `EVENT_ACTIVITY`
- `CALL_FOR_PARTICIPATION`
- `ADMINISTRATIVE_NOTICE`
- `WELLBEING_SUPPORT`
- `RECOGNITION_ACHIEVEMENT`
- `GENERAL_OTHER`

### Major human decisions recorded
- Exactly one primary category per email; no runner-up category is exposed in V1.
- A genuine unresolved tie returns `GENERAL_OTHER` with `LOW` confidence.
- Conceptual evidence weights are strong subject 3, strong current body 2, medium/context 1, and filtered zones/sender identity 0.
- Duplicate logical evidence is counted once; keyword repetition and repeated quotes do not boost scores.
- Internal confidence maps to persisted/API values as `HIGH` 0.9, `MEDIUM` 0.7, and `LOW` 0.5; these are deterministic levels, not calibrated probabilities.
- Primary-purpose rules distinguish career, administration, academic/research, wellbeing support, participation calls, events, completed recognition, and fallback without a blind enum order.
- `WELLBEING_SUPPORT` is narrow; an ordinary public yoga/meditation/wellness gathering is `EVENT_ACTIVITY` unless it is explicitly part of an institute support service.
- Institute facility/service feedback may be administrative; generic club surveys, research questionnaires, and opinion polls are not automatically administrative.
- Non-selective academic registration is academic; selective/competitive programme applications are participation calls unless employment makes them career-related.
- Recognition requires completed-state evidence; future prizes, award applications, and competition calls are excluded.
- Current content is scored first; quote-only rescue is capped at `MEDIUM` and cannot override conflicting specific current evidence.
- Signatures, footers, boilerplate, URLs, addresses, and social links are masked for evidence only; original stored content remains unchanged.
- V1 supports case/Unicode/whitespace/punctuation/prefix normalisation, word/phrase boundaries, and duplicate evidence suppression rather than arbitrary substring matching.
- Negation support is limited to simple nearby forms and is documented as an edge-case extension.
- Attachment content is outside V1; attachment-only meaning falls back to `GENERAL_OTHER`/`LOW`.

### Work completed
- Created the human-approved V1 source of truth with normative categories, scoring, confidence mapping, purpose boundaries, evidence zones, quote/filtering/normalisation/negation rules, attachment limitation, decision procedure, test plan, acceptance criteria, and change control.
- Translated the discovery test matrix into approved V1 planned tests without implementing JUnit code.
- Corrected `wellness_activity_tie` to `EVENT_ACTIVITY`/`HIGH` for an ordinary public gathering without support-service context and added the corresponding explicit-support case.
- Standardised all `GENERAL_OTHER` fallback expectations to `LOW`/0.5.
- Marked the first 20 classifier tests and V1 assertions as `PLANNED`.
- Added a supersession/status note to the historical discovery artifact while preserving its discovery and validation evidence.
- Appended the concise prompt summary and this detailed traceability entry.

### Files created/modified
- `docs/CLASSIFICATION_SPEC_V1.md` (created)
- `docs/EMAIL_CLASSIFICATION_DISCOVERY.md` (updated with V1 supersession note and approved wellness expectation)
- `docs/TEST_LOG.md` (updated; V1 tests marked `PLANNED`)
- `docs/AGENT_PROMPTS.md` (appended concise prompt summary)
- `docs/WORK_LOG.md` (appended this entry)
- `docs/BUG_LOG.md` was not modified; no genuine bug was discovered.

### Implementation status
**NOT STARTED**

- No Java production source was modified.
- No Java test source was modified.
- No classifier or JUnit test was implemented.
- No database schema, Gmail behavior, profile/ranking logic, or Python reference code was modified.

### Tests and checks
- Documentation whitespace check: `git diff --check` — passed.
- Scope check: `git status --short` filtered for paths outside `docs/` — 0 non-documentation changes.
- Final complete Java baseline after documentation changes: `.\\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -Ddebug=false test` — BUILD SUCCESS; 77 tests run, 0 failures, 0 errors, 0 skipped in 27.761 seconds.
- Existing expected negative-test log messages and JVM warning appeared but caused no failures.

### Git
Commit: Pending
PR: Pending
PR created by: Pending

### Next step
Implement the first approved classifier JUnit tests from `docs/CLASSIFICATION_SPEC_V1.md` and `docs/TEST_LOG.md` before writing classifier production code; confirm the new tests fail for the expected missing implementation, then implement the smallest deterministic Java classifier needed to make them pass.

---

## Work Entry
Date: 2026-10-06
Contributor: Codex
Milestone: Deterministic Email Classification
Stage: Classifier Test Batch 1 — RED Phase
Branch: phase4-live-demo

### Status
Completed — expected RED test-first state.

### Starting state
- Starting commit: `8264fe1b4980642ae9f9d4db0d27fda32a160e1b`.
- Approved source: `docs/CLASSIFICATION_SPEC_V1.md`.
- Existing documentation changes from the preceding discovery/specification tasks were preserved.
- Baseline command: `.\\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -Ddebug=false test`.
- Baseline result before Batch 1 changes: BUILD SUCCESS; 77 tests run, 0 failures, 0 errors, 0 skipped in 25.202 seconds.

### Work completed
- Inspected the approved V1 specification, discovery evidence, test log, `pom.xml`, Java source layout, DTO/service conventions, and existing JUnit 5/AssertJ test style.
- Added exactly 20 individually named synthetic/anonymised classifier tests with clear Arrange–Act–Assert structure.
- Covered the eight core category examples, generic-keyword false positives, primary-purpose precedence, quote handling, null/empty input, and Unicode-hyphen normalisation.
- Each test asserts both the expected primary category and confidence class.
- Added the smallest compile contract needed to execute the tests: category enum, confidence enum, immutable result record, service interface, and concrete rule-based service skeleton.
- The concrete skeleton deliberately throws `UnsupportedOperationException`; it contains no classification rules or fake outputs.
- Appended the concise prompt summary and detailed test/work traceability.

### Files created/modified
- `src/test/java/edu/campussignal/RuleBasedEmailClassificationServiceTest.java` (created; 20 tests)
- `src/main/java/edu/campussignal/dto/EmailCategory.java` (created; compile contract)
- `src/main/java/edu/campussignal/dto/ClassificationConfidence.java` (created; compile contract)
- `src/main/java/edu/campussignal/dto/EmailClassificationResult.java` (created; immutable compile contract)
- `src/main/java/edu/campussignal/service/EmailClassificationService.java` (created; service contract)
- `src/main/java/edu/campussignal/service/RuleBasedEmailClassificationService.java` (created; deliberately unimplemented skeleton)
- `docs/TEST_LOG.md` (updated with all 20 RED cases and results)
- `docs/CLASSIFICATION_SPEC_V1.md` (updated to record Batch-1 RED status and distinguish confident out-of-taxonomy `GENERAL_OTHER` from Low-confidence fallback)
- `docs/AGENT_PROMPTS.md` (appended concise task summary)
- `docs/WORK_LOG.md` (appended this entry)
- `docs/BUG_LOG.md` was not modified; expected RED tests are not bugs.

### RED-phase result
- Focused command: `.\\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" "-Dtest=RuleBasedEmailClassificationServiceTest" -Ddebug=false test`.
- Result: BUILD FAILURE; 20 tests run, 0 failures, 20 errors, 0 skipped.
- Failure mode: tests compiled and executed; every error was the intentional `UnsupportedOperationException` from `RuleBasedEmailClassificationService.classify` because business logic is absent.
- This is not a compile failure and not an unexpected product bug.

### Existing-suite result
- Command excluding the intentional RED class: `.\\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" "-Dtest=!RuleBasedEmailClassificationServiceTest" -Ddebug=false test`.
- Result: BUILD SUCCESS; 77 tests run, 0 failures, 0 errors, 0 skipped in 25.166 seconds.
- Existing expected negative-test log messages and JVM warning appeared but caused no failures.

### Implementation status
- Classifier compile contract: present.
- Classifier decision/business logic: **NOT IMPLEMENTED**.
- Gmail integration, profile/category scoring, relevance ranking, database schema, and Python reference code: unchanged.

### Git
Commit: Pending
PR: Pending
PR created by: Pending

### Next step
Implement the smallest deterministic rule-based behavior in `RuleBasedEmailClassificationService` required by the approved V1 specification to make these 20 tests GREEN, without weakening or changing the tests; then run the focused classifier tests followed by the complete suite.

---

## Work Entry
Date: 2026-10-06
Contributor: Codex
Milestone: Deterministic Email Classification
Stage: Classifier Test Batch 1 — GREEN Phase
Branch: phase4-live-demo

### Status
Completed — Batch-1 classifier behavior is GREEN.

### Starting state
- Starting commit: `8264fe1b4980642ae9f9d4db0d27fda32a160e1b`.
- Approved source: `docs/CLASSIFICATION_SPEC_V1.md`.
- Test contract: `src/test/java/edu/campussignal/RuleBasedEmailClassificationServiceTest.java` with exactly 20 tests; no test was changed during this task.
- Pre-change classifier result: BUILD FAILURE; 20 tests run, 0 failures, 20 intentional errors, 0 skipped because the service skeleton threw `UnsupportedOperationException`.
- Pre-change unrelated-suite result: BUILD SUCCESS; 77 tests run, 0 failures, 0 errors, 0 skipped.
- Existing uncommitted discovery, specification, test-contract, and documentation work was preserved.

### Work completed
- Replaced the deliberate classifier skeleton with the smallest explicit deterministic implementation required by Batch 1.
- Added null-safe case/Unicode/whitespace/punctuation normalisation, Unicode dash handling, word/phrase-boundary matching, URL/email masking, common trailing-zone filtering, and reply/forward-prefix handling.
- Added simple current-content/quoted-content segmentation; current specific evidence wins, while quote-only rescue is capped at `MEDIUM`.
- Added category-purpose evidence for academic/research, career, events, participation calls, administrative notices, wellbeing support, completed recognition, and confident out-of-taxonomy general purposes.
- Applied conceptual weights of 3/2/1 for strong subject, strong body, and contextual evidence, with each logical evidence flag counted once.
- Implemented explicit purpose gates so career, academic, administrative, wellbeing, participation, and completed recognition evidence override generic event/call wording where specified.
- Enforced the recognition completed-state gate so future prize pools and contest registrations do not become achievements.
- Preserved `GENERAL_OTHER`/`LOW` for empty or insufficient evidence.

### Implementation iterations and issues
- Iteration 1: the initial implementation passed all 20 focused tests with 0 failures and 0 errors.
- A subsequent code review found genuine issue `CS-CLS-001`: reply/forward-prefix removal ran after punctuation normalisation, leaving a harmless but non-compliant leading token.
- Iteration 2: reordered subject preprocessing, then reran both focused and complete suites successfully.
- No JUnit assertion failure occurred during GREEN implementation; the normalization defect was found by review rather than by weakening or changing a test.

### Files modified
- `src/main/java/edu/campussignal/service/RuleBasedEmailClassificationService.java`
- `docs/CLASSIFICATION_SPEC_V1.md` (implementation-status metadata only; approved decisions unchanged)
- `docs/AGENT_PROMPTS.md`
- `docs/WORK_LOG.md`
- `docs/TEST_LOG.md`
- `docs/BUG_LOG.md` (genuine issue `CS-CLS-001` only)

### Verification
- Focused command: `.\\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" "-Dtest=RuleBasedEmailClassificationServiceTest" -Ddebug=false test`.
- Initial GREEN result: BUILD SUCCESS; 20 tests run, 0 failures, 0 errors, 0 skipped.
- Final focused result after `CS-CLS-001` fix: BUILD SUCCESS; 20 tests run, 0 failures, 0 errors, 0 skipped in 14.136 seconds.
- Complete command: `.\\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -Ddebug=false test`.
- Final complete result: BUILD SUCCESS; 97 tests run, 0 failures, 0 errors, 0 skipped in 26.120 seconds.
- Existing expected negative-test log messages and JVM/deprecation warnings appeared but caused no failures.

### Scope and implementation status
- Batch-1 classifier business logic: **IMPLEMENTED and GREEN**.
- Existing Batch-1 tests: unchanged, enabled, and 20/20 passing.
- Gmail integration, email persistence/database schema, controllers/API, profile/category scoring, relevance ranking, and Python reference code: unchanged.
- No commit or push was created.

### Git
Commit: Pending
PR: Pending
PR created by: Pending

### Next step
Create the next approved test-first robustness batch before expanding the rules, prioritising signature/footer/URL filtering, simple negation, exact ties and duplicate evidence, normal wellness activities versus support services, facility surveys, selective versus non-selective academic registration, HTML text, and a dedicated reply/forward-prefix regression test.

---

## Work Entry
Date: 2026-10-06
Contributor: Codex
Milestone: Deterministic Email Classification
Stage: Classifier Robustness Test Batch 2 — Initial RED/GREEN Baseline
Branch: phase4-live-demo

### Status
Completed — 20 robustness tests added and executed against the unchanged Batch-1 classifier.

### Starting state
- Starting commit: `cdce087bfc7b05222a1fa10caf0762e5223fb7b5`.
- Working tree before this task: clean; the previous Batch-1 GREEN state was committed.
- Baseline command: `.\\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -Ddebug=false test`.
- Baseline result: BUILD SUCCESS; 97 tests run, 0 failures, 0 errors, 0 skipped in 27.274 seconds.
- Existing composition: 77 original Java tests plus 20 Batch-1 classifier tests.

### Work completed
- Re-read the human-approved V1 specification, discovery evidence, test/bug logs, existing 20 tests, and current Batch-1 implementation.
- Added exactly 20 individual deterministic JUnit 5 tests to the existing classifier test class.
- Covered reply/forward prefix normalisation, signature/footer/URL/address filtering, simple negation, future-recognition boundaries, unresolved ties, repeated evidence, simple HTML, facility surveys, academic-registration selection boundaries, and wellbeing/event boundaries.
- Used synthetic/anonymised inputs only; no Gmail, database, network, private mailbox, or time-dependent data is involved.
- Did not modify any Batch-1 test or any production source.

### Files changed
- `src/test/java/edu/campussignal/RuleBasedEmailClassificationServiceTest.java` (20 Batch-2 methods appended; original 20 preserved)
- `docs/AGENT_PROMPTS.md`
- `docs/TEST_LOG.md`
- `docs/WORK_LOG.md`

### Initial Batch-2 result
- Focused command: `.\\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" "-Dtest=RuleBasedEmailClassificationServiceTest" -Ddebug=false test`.
- Combined classifier result: BUILD FAILURE; 40 tests run, 6 failures, 0 errors, 0 skipped in 18.771 seconds.
- Batch-2 result: 14 passing, 6 failing, 0 erroring.
- Failing tests: `urlKeywordIgnored`, `negatedInternship`, `negatedEvent`, `exactTieFallsBackToGeneral`, `facilityFeedbackIsAdministrative`, and `publicYogaActivityIsEvent`.
- Failure patterns: missing facility-feedback qualification, missing nearby-negation suppression, no genuine-tie detection, and wellbeing false positives from negated support wording. `negatedEvent` already returns the correct academic category but produces `HIGH` instead of approved `MEDIUM` confidence.

### Regression verification
- Original Batch-1 tests were run in isolation after the combined run.
- Result: BUILD SUCCESS; 20 tests run, 0 failures, 0 errors, 0 skipped in 12.092 seconds.
- The 77 original non-classifier tests were green in the pre-change full-suite baseline; production code remained unchanged afterward.

### Scope and implementation status
- Production classifier logic: **UNCHANGED**.
- Gmail integration, persistence/entities, database schema, controllers, profile/category scoring, ranking, and Python reference code: unchanged.
- `docs/BUG_LOG.md`: unchanged; newly exposed V1 gaps are test-first specification gaps, not implementation bugs at this stage.
- No commit or push was created.

### Git
Commit: Pending
PR: Pending
PR created by: Pending

### Next step
In a separate GREEN implementation task, make the smallest V1-compliant production changes for the six failing Batch-2 cases: add facility/service-feedback evidence, simple nearby-negation suppression, explicit unresolved-tie fallback, and correct confidence handling; then rerun all 40 classifier tests followed by the complete Java suite without changing test expectations.

---

## Work Entry
Date: 2026-10-06
Contributor: Codex
Milestone: Deterministic Email Classification
Stage: Classifier Test Batch 2 — GREEN Phase
Branch: phase4-live-demo

### Status
Completed — all 40 classifier tests and all 117 Java tests are GREEN.

### Starting state
- Task-start commit: `fdaf5258cfdb50eb94dd7a5f49f5d6ade3da8059`.
- Historical commit before the Batch-2 tests: `cdce087bfc7b05222a1fa10caf0762e5223fb7b5`.
- Working tree at task start: clean; the 20 Batch-2 tests and their initial-result documentation had been committed.
- Reproduction command: `.\\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" "-Dtest=RuleBasedEmailClassificationServiceTest" -Ddebug=false test`.
- Reproduced result: BUILD FAILURE; 40 tests run, 6 failures, 0 errors, 0 skipped in 8.352 seconds.
- Reproduced failures: `urlKeywordIgnored`, `negatedInternship`, `negatedEvent`, `exactTieFallsBackToGeneral`, `facilityFeedbackIsAdministrative`, and `publicYogaActivityIsEvent`; actual and expected results matched the recorded Batch-2 baseline.

### Root-cause analysis
- `CS-CLS-002`: URL/address masking was already correct. Two failures shared a missing rule that promotes specific institute facility/service operational feedback to administrative evidence while excluding generic surveys.
- `CS-CLS-003`: three failures shared absent nearby-negation handling. Negated career/wellbeing cues were counted positively, and resolved negation did not cap confidence at `MEDIUM`.
- `CS-CLS-004`: equal independent academic/event evidence was resolved by early rule order instead of the approved `GENERAL_OTHER`/`LOW` tie fallback.
- No separate generic-wellness defect was found: `wellness` and `yoga` alone already did not qualify as wellbeing; the false positive came from negated counselling/support wording.

### Implementation iterations
1. Added a conjunctive facility-feedback rule requiring feedback, an institute facility/service, and operational context. Focused result: 3 tests run, 0 failures, 0 errors, including the generic-survey negative control.
2. Added a small nearby-negation window, unnegated matching for affected evidence, `applications are open` participation wording, and a `MEDIUM` cap for results involving resolved negation. Negation regression result: 6 tests run, 0 failures, 0 errors.
3. Added explicit equal-score fallback when academic and event purposes both have independent strong subject/body evidence. Tie/precedence result: 4 tests run, 0 failures, 0 errors.
4. Verified the wellbeing/event boundary after the negation fix; no additional production patch was required. Result: 4 tests run, 0 failures, 0 errors.

### Files modified
- `src/main/java/edu/campussignal/service/RuleBasedEmailClassificationService.java`
- `docs/AGENT_PROMPTS.md`
- `docs/BUG_LOG.md`
- `docs/TEST_LOG.md`
- `docs/WORK_LOG.md`
- `docs/CLASSIFICATION_SPEC_V1.md` (implementation-status metadata only; approved behavior unchanged)

### Final verification
- Classifier command: `.\\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" "-Dtest=RuleBasedEmailClassificationServiceTest" -Ddebug=false test`.
- Classifier result: BUILD SUCCESS; 40 tests run, 0 failures, 0 errors, 0 skipped in 7.277 seconds.
- Complete command: `.\\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -Ddebug=false test`.
- Complete result: BUILD SUCCESS; 117 tests run, 0 failures, 0 errors, 0 skipped in 26.634 seconds.
- Existing expected negative-test log messages and JVM/deprecation warnings appeared but caused no failures.

### Scope and implementation status
- No Batch-1 or Batch-2 test was modified, weakened, disabled, renamed, or skipped.
- Gmail integration, persistence/entities, database schema, controllers/API, profile/category scoring, relevance ranking, and Python reference code were untouched.
- No external NLP library, ML/LLM classifier, rule framework, or unrelated refactor was introduced.
- No commit or push was created.

### Remaining limitations
- Negation handling is deliberately bounded to a small nearby-token window and approved category cues; it is not general language understanding.
- Tie detection currently addresses independently strong equal academic/event purposes exercised by V1 tests; broader multi-category combinations need additional test-first evidence before expansion.
- HTML handling remains basic normalisation rather than a full MIME/HTML extraction layer.
- Attachment-only meaning remains outside V1 and falls back to `GENERAL_OTHER`/`LOW`.

### Git
Commit: Pending
PR: Pending
PR created by: Pending

### Next step
Create the next test-first V1 coverage batch for the still-planned cases—especially attachment-only fallback, unusual whitespace/case variants, footer/signature variants, additional completed-recognition boundaries, and broader conflict combinations—before classifier integration with ingestion or persistence.

---
