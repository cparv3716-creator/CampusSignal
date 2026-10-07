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
Date: 2026-10-07
Contributor: Codex
Milestone: CS5013 Code Coverage
Stage: Final Classifier Coverage Batch
Branch: phase4-live-demo

### Status
Completed — exactly two specification-backed classifier tests were added and passed against the existing production implementation.

### Starting state
- Classifier tests: 43.
- Total Java tests: 135 passing; 0 failures, 0 errors, 0 skipped.
- Overall coverage: 697/754 lines (92.44%) and 369/441 branches (83.67%).
- `RuleBasedEmailClassificationService`: 87/102 branches (85.29%).
- `ClassificationTextPreprocessor`: 43/46 branches (93.48%).

### Implementation and test-first execution
- Inspected `RuleBasedEmailClassificationService.eventEvidence`, `ClassificationTextPreprocessor` negation handling, `CLASSIFICATION_SPEC_V1.md`, the original 43 classifier tests, and the JaCoCo line-level branch report.
- Added `subjectNegationSuppressesCareerCueWithoutHidingAcademicPurpose`; its isolated first run passed 1/1, and the classifier class passed 45/45.
- Added `namedActivityWithSubjectScheduleIsAnEvent`; its isolated first run passed 1/1, and the classifier class passed 45/45.
- The subject test confirms suppression of a negated specialist cue, continued classification from valid current-message evidence, V1 confidence downgrade after resolved negation, and preservation of the separately asserted four-token window.
- The event test confirms the approved attendance-language, named-activity, and concrete-logistics subject rule with subject-only `MEDIUM` confidence.
- No genuine defect was exposed. Production Java and `docs/BUG_LOG.md` were not changed.

### Verification
- Final focused classifier result: BUILD SUCCESS — 45 tests, 0 failures, 0 errors, 0 skipped.
- Final full command: `.\mvnw.cmd -B -ntp clean test`.
- Final full result: BUILD SUCCESS — 137 tests, 0 failures, 0 errors, 0 skipped in 49.145 seconds.
- JaCoCo overall branches: 369/441 (83.67%) → 372/441 (84.35%).
- `RuleBasedEmailClassificationService` branches: 87/102 (85.29%) → 90/102 (88.24%).
- `ClassificationTextPreprocessor` branches: unchanged at 43/46 (93.48%).
- Newly covered outcomes: subject-side negation short-circuit at classifier line 42; composite subject-event evaluation at line 204; schedule-present true outcome at line 205.
- No JaCoCo threshold was introduced.

### Files
- Test Java: `src/test/java/edu/campussignal/RuleBasedEmailClassificationServiceTest.java`.
- Documentation: `docs/AGENT_PROMPTS.md`, `docs/COVERAGE_LOG.md`, `docs/TEST_LOG.md`, and `docs/WORK_LOG.md`.
- Production Java: unchanged.
- `docs/BUG_LOG.md`: unchanged.

### Git
Commit: Pending
PR: Pending
PR created by: Pending

### Next step
Stop coverage-driven work and begin classifier integration with the Java email ingestion and persistence flow, preserving source independence and duplicate protection.

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

## Work Entry
Date: 2026-10-06
Contributor: Codex
Milestone: CS5013 Static Analysis
Stage: Initial SpotBugs Configuration and Triage
Branch: phase4-live-demo

### Status
Completed — initial unsuppressed SpotBugs report generated and every finding triaged; no production fix applied.

### Starting state
- Starting commit: `cb7bb4f97869a725444bbca5d3751bd22439f953`.
- Starting working tree: clean.
- Java runtime: `26.0.2`; project target: Java 25.
- Baseline command: `.\\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -Ddebug=false test`.
- Baseline result: BUILD SUCCESS; 117 tests run, 0 failures, 0 errors, 0 skipped in 32.341 seconds.

### Tooling configuration
- Added `com.github.spotbugs:spotbugs-maven-plugin:4.10.4.1` to `pom.xml`.
- Configured `effort=Max`, `threshold=Low`, XML output, and `failOnError=true`.
- Added no exclusion filter, suppression file, detector extension, PMD, JaCoCo, or unrelated plugin.
- SpotBugs engine version reported by the generated XML: `4.10.4`.

### Analysis execution
- Command: `.\\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -DskipTests compile spotbugs:spotbugs`.
- Result: BUILD SUCCESS in 1 minute 12 seconds.
- Initial report: 6 unsuppressed findings across 50 analysed classes.
- Categories: 2 `MALICIOUS_CODE`, 3 `STYLE`, 1 `BAD_PRACTICE`.
- Priorities: 1 Priority 1, 4 Priority 2, 1 Priority 3.

### Triage result
- TRUE POSITIVE — likely real defect: none confirmed.
- CODE QUALITY / MAINTAINABILITY: 3 findings — two NPE-catching input parsers and one broad checked-exception functional contract.
- FALSE POSITIVE: 2 findings — unmodifiable `Stream.toList()` record state and trusted Spring `ObjectMapper` injection.
- NEEDS FURTHER INVESTIGATION: 1 finding — broad exception handling at the Gmail CLI privacy/error boundary.
- No warning was suppressed or fixed. Detailed per-finding reasoning, proposed fixes, and test implications are recorded in `docs/STATIC_ANALYSIS_LOG.md`.
- `docs/BUG_LOG.md` was not modified because static-analysis candidates are not automatically confirmed application bugs.

### Files created/modified
- `pom.xml` — SpotBugs plugin configuration only.
- `docs/STATIC_ANALYSIS_LOG.md` — created with the initial report and full triage.
- `docs/AGENT_PROMPTS.md` — appended concise task summary.
- `docs/WORK_LOG.md` — appended this entry.
- Production Java and Java test files: unchanged.

### Implementation status
- Application behavior: unchanged.
- Classifier rules, Gmail integration, persistence, schema, controllers, profile scoring, and ranking: unchanged.
- Static-analysis fixes: not started, pending human review.
- No commit or push was created.

### Regression verification
- Command: `.\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -Ddebug=false test`
- Result: BUILD SUCCESS; 117 tests run, 0 failures, 0 errors, 0 skipped in 26.721 seconds.
- The SpotBugs configuration and documentation changes did not affect existing behaviour.

### Git
Commit: Pending
PR: Pending
PR created by: Pending

### Next step
Review and approve the SA-003 Gmail command-boundary policy. Then add focused regression tests and implement only approved fixes for SA-002, SA-005, and SA-006; rerun SpotBugs unsuppressed before considering narrow documented suppressions for SA-001 and SA-004.

---

## Work Entry
Date: 2026-10-06
Contributor: Codex
Milestone: CS5013 Static Analysis
Stage: Test-First SpotBugs Remediation
Branch: phase4-live-demo

### Status
Completed — four justified findings removed without blanket suppressions; two documented false positives remain.

### Starting state
- Verified branch: `phase4-live-demo`.
- Verified Java baseline: 117 tests, 0 failures, 0 errors, 0 skipped.
- SpotBugs baseline: plugin 4.10.4.1, engine 4.10.4, 6 unsuppressed findings.
- The existing uncommitted initial-analysis changes in `pom.xml`, `docs/STATIC_ANALYSIS_LOG.md`, `docs/AGENT_PROMPTS.md`, and `docs/WORK_LOG.md` were preserved.
- Production Java initially contained no SpotBugs-driven changes.

### Group 1 — history-ID parsing
- Identified `GmailApiClient.historyId(String)` and `GmailHistorySyncService.historyId(String)` as the two NPE-catching parsers.
- Confirmed null sources: a direct/missing start history ID at the Gmail client boundary and a missing notification history ID at the synchronization boundary. Malformed and negative values are also invalid under the existing contract.
- Added two client parser tests and one service-level invalid-notification test before production changes.
- Pre-change characterization passed 3/3.
- Replaced caught-NPE control flow with explicit null rejection and retained `NumberFormatException` handling and established `IllegalArgumentException` messages.
- Post-change focused tests passed 3/3; relevant regression classes passed 14/14.

### Group 2 — Pub/Sub handler contract
- Inspected the handler and its sole application caller. The only checked implementation failure is `IOException` from Gmail history synchronization; decoder and programming failures are unchecked.
- Added `nacksCheckedIoFailure` before changing the interface; the existing success and unchecked-failure tests already covered the other dispatch paths.
- Pre-change `PubSubPullSubscriberTest` passed 3/3.
- Narrowed `Handler.handle(byte[])` from `throws Exception` to `throws IOException` and narrowed dispatch handling to `IOException | RuntimeException`.
- Post-change focused test passed 1/1; full Pub/Sub regression class passed 3/3.

### Group 3 — Gmail command privacy/diagnostic boundary
- Existing `errorsAreReportedWithoutProviderDetails` already proved sanitized handling for an expected checked `IOException`.
- Added a test requiring unexpected runtime failure to remain non-successful and diagnostically distinguishable without exposing its sensitive message.
- Initial RED result: 1 test run, 1 failure because the original broad catch emitted no runtime diagnostic.
- Split expected checked failures (`IOException | GeneralSecurityException`) from unexpected `RuntimeException`.
- Retained the outer privacy boundary, sanitized user-visible message, and exit code 1.
- Added safe internal logging containing only the selected command and runtime exception class; exception messages, credentials, tokens, Pub/Sub payloads, and mailbox content are not emitted.
- The first post-change assertion attempted to read the logger through Spring output capture; the event appeared in the console but was not retained by that capture mechanism. The test was corrected to inspect a temporary Logback appender. No production change was made for this harness issue.
- Final focused tests passed 2/2; complete `GmailCommandTest` passed 7/7.

### New tests
- `GmailApiClientTest.parsesValidHistoryIdWithoutLosingPrecision`
- `GmailApiClientTest.rejectsMissingMalformedAndNegativeHistoryIds`
- `GmailRealtimeServiceTest.invalidNotificationHistoryIdsFailWithoutAdvancingProgress`
- `PubSubPullSubscriberTest.nacksCheckedIoFailure`
- `GmailCommandTest.unexpectedRuntimeFailureIsDiagnosableWithoutLeakingSensitiveDetails`

### Production files changed
- `src/main/java/edu/campussignal/gmail/GmailApiClient.java`
- `src/main/java/edu/campussignal/service/GmailHistorySyncService.java`
- `src/main/java/edu/campussignal/pubsub/PubSubPullSubscriber.java`
- `src/main/java/edu/campussignal/gmail/GmailCommand.java`

### Complete verification
- Command: `.\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -Ddebug=false test`.
- Result: BUILD SUCCESS; **122 tests run, 0 failures, 0 errors, 0 skipped in 27.292 seconds**.
- SpotBugs command: `.\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -DskipTests compile spotbugs:spotbugs`.
- Result: BUILD SUCCESS in 26.579 seconds.
- Findings before/after: **6 → 2** across 50 analysed classes.
- Removed: SA-002 and SA-006 (`DCN_NULLPOINTER_EXCEPTION`), SA-005 (`THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION`), and SA-003 (`REC_CATCH_EXCEPTION`).
- Remaining: SA-001 (`EI_EXPOSE_REP`) and SA-004 (`EI_EXPOSE_REP2`), both still triaged as false positives with no production changes or suppressions.

### Defect and scope record
- Investigation confirmed one genuine operational behavior defect: unexpected Gmail command runtime failures were silently indistinguishable from expected operational failures. It is recorded as `CS-STA-001` and fixed.
- The two history parser findings and Pub/Sub signature finding remained maintainability issues rather than observed runtime product defects.
- Classifier rules, ranking, Gmail retrieval/ingestion semantics, database schema, profiles/scoring, Python, PMD, and JaCoCo were untouched.
- No broad or narrow SpotBugs suppression was added.
- No commit or push was created.

### Git
Commit: Pending
PR: Pending
PR created by: Pending

### Next step
Review and approve narrow, documented per-finding suppressions for SA-001 and SA-004 only if a zero-finding SpotBugs gate is required; otherwise retain the unsuppressed two-finding report as the accepted baseline.

---

## Work Entry
Date: 2026-10-06
Contributor: Codex
Milestone: CS5013 Static Analysis
Stage: Initial PMD Maintainability Analysis and Triage
Branch: phase4-live-demo

### Status
Completed — reproducible PMD tooling added, 32 unsuppressed production-Java findings triaged, and no production or test code changed.

### Starting state
- Commit: `4544610b5341f692a93006fe8bb156a7de65897f`.
- Starting tracked worktree: clean.
- Initial ordinary test command: `.\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -Ddebug=false test`.
- Initial result: BUILD FAILURE; 122 tests run, 0 failures, 45 errors, 0 skipped in 48.961 seconds. The errors were missing compiled classes in an incomplete/stale `target/classes`, while Maven incorrectly considered compilation up to date.
- Clean baseline command: `.\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -Ddebug=false clean test`.
- Authoritative baseline: BUILD SUCCESS; 122 tests run, 0 failures, 0 errors, 0 skipped in 1 minute 18 seconds. The clean compile rebuilt all 41 production source files without a tracked source change.
- The stale build-output incident was not logged as an application bug.

### PMD setup
- Added Maven PMD Plugin `3.28.0` without changing the existing SpotBugs plugin.
- Plugin 3.28.0's bundled PMD 7.17.0 could not read JDK 26 class-file major version 70, so PMD runtime modules were reproducibly overridden to stable PMD `7.27.0` through Maven plugin dependencies.
- Configured Java target 25, production source only, minimum priority 5, no XRef, no suppressions, and no exclusions.
- Added `config/pmd/ruleset.xml`: PMD Java quickstart plus focused design rules for deep nesting, cognitive/cyclomatic/NPath complexity, NCSS size, collapsible conditions, parameter count, God Class, boolean simplification, and method count.
- PMD 7 uses `NcssCount` in place of the removed excessive class/method length rules.
- No JaCoCo, JFR, JMC, standalone PMD download, or unrelated tool was added.

### Tooling resolution
- Bundled PMD 7.17.0 run: invalid because it failed on JDK 26 runtime classes.
- PMD 7.27.0 quickstart run: valid but incomplete for the requested complexity scope; 23 preliminary findings.
- Directly listing individual design rules in Maven `<rulesets>`: invalid 223-item report because the Maven plugin repeated the full category for each entry; discarded.
- Final checked-in composite ruleset: valid report with no processing errors and no duplicated rule execution.

### Initial PMD result
- Command: `.\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -DskipTests compile pmd:pmd`.
- Result: BUILD SUCCESS in 19.100 seconds.
- PMD runtime: 7.27.0.
- Findings: 32 unsuppressed findings across production Java.
- Categories: 19 Best Practices, 9 Design, 3 Documentation, 1 Error Prone.
- Priorities: 2 Priority 2 and 30 Priority 3.
- Triage: 5 real maintainability issues, 8 minor style issues, 17 false positives/not applicable, and 2 needing further investigation.

### Important triage outcomes
- Real issues: combined pagination responsibilities in `GmailApiClient.listMessageIds`; combined validation paths in `GmailNotificationDecoder.decode`; nested provider traversal in `GmailHistorySyncService.messageAddedIds`; and excessive method count in `RuleBasedEmailClassificationService`.
- The classifier's `GodClass` result is not applicable as a responsibility diagnosis: the class is stateless and cohesive around deterministic classification, though its method count is a useful structural signal.
- `classifyCurrent` is exactly at cyclomatic threshold 10 but remains a readable ordered precedence chain; it has no cognitive, NPath, deep-nesting, or NCSS finding.
- `GmailMessageMapper.collectText` and stale-history cause preservation require more investigation before changes.
- Twelve `LooseCoupling` findings are not applicable to required concrete Google API model types.
- Spring context lifetime, OAuth loopback IP, and cheap privacy-safe error logging findings are not applicable.
- Three JPA constructor comments and three literal-first comparisons are minor style issues.
- No unused/private code, empty catch/block, excessive parameter, NPath, deep nesting, collapsible condition, NCSS, or PMD performance finding was reported.
- PMD complexity findings were recorded as control-flow maintainability signals, not Big-O performance results.

### Documentation and files
- `pom.xml` — added PMD plugin and reproducible PMD 7.27.0 runtime override; SpotBugs unchanged.
- `config/pmd/ruleset.xml` — added the reviewed initial ruleset.
- `docs/PMD_ANALYSIS_LOG.md` — created with all 32 findings, detailed triage, risk, tests, unresolved questions, and remediation order.
- `docs/AGENT_PROMPTS.md` — appended the exact prompt and concise summary.
- `docs/WORK_LOG.md` — appended this execution record.
- `docs/BUG_LOG.md` — unchanged; no genuine behavioral defect was discovered.
- Production Java and Java tests — unchanged.

### Post-tooling regression verification
- Command: `.\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -Ddebug=false test`.
- Result: BUILD SUCCESS; **122 tests run, 0 failures, 0 errors, 0 skipped in 29.183 seconds**.

### Scope confirmation
- Classifier behavior, Gmail behavior, ingestion, database schema, profile scoring, and ranking were untouched.
- Python was not deleted or modified.
- No PMD suppression or exclusion was added.
- No commit or push was created.

### Git
Commit: Pending
PR: Pending
PR created by: Pending

### Next step
Create a separate test-first classifier remediation task: add focused preprocessing and negation-boundary characterization, extract only text preprocessing/negation into a package-private collaborator, preserve category evidence and precedence, run the 40 classifier tests and full suite, and rerun PMD.

---

## Work Entry
Date: 2026-10-06
Contributor: Codex
Milestone: CS5013 Static Analysis
Stage: Test-first PMD Maintainability Remediation
Branch: phase4-live-demo

### Status
Completed — four bounded maintainability refactorings were characterized and verified; two investigation-only findings were deliberately left unchanged.

### Starting state
- Commit: `71e43ca00329ff056e17fca95ed8497b9da9d689`.
- Starting worktree: clean.
- Baseline command: `.\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" clean test`.
- Baseline result: BUILD SUCCESS; **122 tests run, 0 failures, 0 errors, 0 skipped in 40.922 seconds**.
- PMD command: `.\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -DskipTests compile pmd:pmd`.
- Baseline PMD result: BUILD SUCCESS; **32 findings across 13 classes** with Maven PMD Plugin 3.28.0 and PMD 7.27.0.

### Gmail message-list pagination
- Reviewed all existing `GmailApiClientTest` coverage and identified missing blank/duplicate ID and blank-token characterization.
- Added two tests; pre-refactor focused result: 11/11 passing.
- Refactored ID collection and pagination-state validation without adding network calls or changing maximum-result, duplicate, failure, or token behavior.
- An initial four-helper shape passed tests but raised PMD from 32 to 35 by introducing one `GodClass` and four `LooseCoupling` signals. It was immediately simplified to two cohesive interface-based helpers.
- Final verification: 11/11 focused; 124/124 complete; both original complexity findings disappeared; PMD 32 → 30.

### Gmail notification decoding
- Added three tests for blank decoded data, missing required fields with exact established messages, and non-numeric textual history IDs; pre-refactor focused result: 5/5.
- Confirmed malformed Base64 is outside the decoder boundary because Google Pub/Sub supplies decoded `ByteString` bytes. Malformed decoded payload data remains protected by invalid-JSON handling and Pub/Sub nack tests.
- Extracted JSON-object parsing, email validation, and history-ID validation while retaining all accepted/rejected formats and sanitized exceptions.
- Verification: 5/5 focused; 127/127 complete; cyclomatic finding disappeared; PMD 30 → 29.

### Gmail history message-added traversal
- Added two Spring integration tests covering empty history, missing messages-added lists, null messages/IDs, blank IDs, duplicates across pages, stable progress, and no unnecessary fetches.
- The first test compilation attempt exposed one missing parenthesis in the new fixture; it was corrected before production modification, after which 7/7 characterization tests passed.
- Replaced nested null-aware loops with ordered hierarchy traversal, null-to-empty collection handling, ID filtering, and encounter-order deduplication.
- Verification: 7/7 focused; 129/129 complete; cognitive finding disappeared; PMD 29 → 28.

### Classifier preprocessing extraction
- Added three behavior-level tests for repeated reply/forward prefixes with Unicode normalization, forwarded-message quote separation, and the exact four-token negation boundary. Pre-refactor result: 43/43.
- Added package-private `ClassificationTextPreprocessor` and moved only normalization, filtering, quote/footer segmentation, phrase matching, and negation scanning.
- Category evidence lists, score weights, confidence mapping, tie behavior, and precedence were untouched. All original 40 classifier tests remained unchanged.
- The first extraction left `TooManyMethods` on both classes. Consolidating duplicate positive/negated occurrence scans removed the collaborator finding without changing behavior.
- Final classifier verification: 43/43 focused and 132/132 complete.
- Classifier declared methods reduced from 25 to 14; `TooManyMethods` remains because the threshold is 10. The remaining category/orchestration methods are cohesive, so no metric-driven split was performed. The previously not-applicable `GodClass` finding disappeared.

### Investigation-only decisions
- `GmailMessageMapper.collectText`: left unchanged at cyclomatic complexity 12. The compact recursive MIME traversal is readable and behavior-sensitive. Future characterization should cover disposition-only attachments, multiple fragments/traversal order, null/empty nested bodies, and invalid charset fallback before reconsidering extraction.
- Stale-history cause preservation: left unchanged. The domain exception type already diagnoses the expected 404 recovery signal; retaining a provider exception could expose private provider response/request detail through future generic stack logging. Non-404 exceptions still propagate unchanged.

### Final analysis and verification
- PMD final: **27 findings across 13 classes** — 19 Best Practices, 4 Design, 3 Documentation, 1 Error Prone; two Priority 2 and 25 Priority 3.
- Five reports disappeared: four targeted complexity findings and the classifier `GodClass` false-positive signal.
- Remaining target-adjacent reports: classifier `TooManyMethods`, mapper cyclomatic complexity 12, and stale-history `PreserveStackTrace`, each deliberately documented and unsuppressed.
- Final focused results: Gmail API 11, decoder 5, realtime history 7, classifier 43; all green.
- Final clean command: `.\mvnw.cmd -B -ntp clean test`.
- Final complete result: BUILD SUCCESS; **132 tests run, 0 failures, 0 errors, 0 skipped in 37.600 seconds**.
- No genuine behavioral defect was found, so `docs/BUG_LOG.md` was not changed.
- No PMD or SpotBugs configuration, suppression, exclusion, threshold, database schema, profile scoring, ranking, Python code, or externally observable Gmail/classifier behavior was changed.
- No commit or push was created.

### Files
- Production: `GmailApiClient.java`, `GmailNotificationDecoder.java`, `GmailHistorySyncService.java`, `RuleBasedEmailClassificationService.java`, and new `ClassificationTextPreprocessor.java`.
- Tests: `GmailApiClientTest.java`, `GmailNotificationDecoderTest.java`, `GmailRealtimeServiceTest.java`, and `RuleBasedEmailClassificationServiceTest.java`.
- Documentation: `PMD_ANALYSIS_LOG.md`, `TEST_LOG.md`, `AGENT_PROMPTS.md`, and `WORK_LOG.md`.
- `BUG_LOG.md`: unchanged.

### Git
Commit: Pending
PR: Pending
PR created by: Pending

### Next step
Add the four recommended MIME mapper characterizations in a separate test-first task, then reassess one small `collectText` extraction; do not preserve the stale provider cause unless an explicitly privacy-reviewed internal diagnostic sink is introduced.

---

## Work Entry
Date: 2026-10-06
Contributor: Codex
Milestone: CS5013 Code Coverage
Stage: JaCoCo Baseline Analysis and Triage
Branch: phase4-live-demo

### Status
Completed — JaCoCo was added to the normal Maven test lifecycle, the untouched 132-test baseline was measured, and coverage gaps were triaged without adding tests or changing production behavior.

### Starting state
- Commit: `ac81e06b34a210ad8233a3093c7d8143d9c67c18`.
- Starting worktree: clean (`git status --short` produced no output).
- Runtime: Java 26.0.2; Maven compiler release 25.
- Baseline command: `.\mvnw.cmd -B -ntp clean test`.
- Baseline result: BUILD SUCCESS; **132 tests run, 0 failures, 0 errors, 0 skipped in 38.247 seconds**.
- `pom.xml` contained SpotBugs and PMD configuration but no JaCoCo configuration.

### Tooling change
- Added `jacoco-maven-plugin` 0.8.15 using the `jacoco.version` property.
- Configured `prepare-agent` to populate `jacocoArgLine` and composed it with the existing Surefire Mockito agent using late property evaluation.
- Bound `report` to the `test` phase with HTML, XML, and CSV formats.
- Added no JaCoCo `check` execution, minimum threshold, package exclusion, or class exclusion.
- Left SpotBugs and PMD configuration unchanged.

### Coverage execution
- Command: `.\mvnw.cmd -B -ntp clean test`.
- Result: BUILD SUCCESS; **132 tests run, 0 failures, 0 errors, 0 skipped in 50.454 seconds**.
- JaCoCo analyzed 45 production classes.
- Instructions: 4,578/4,891 covered (**93.60%**); 313 missed.
- Lines: 694/754 covered (**92.04%**); 60 missed.
- Branches: 365/441 covered (**82.77%**); 76 missed.
- Methods: 173/187 covered (**92.51%**); 14 missed.
- Classes: 45/45 covered (**100.00%**); 0 missed.
- Reports generated at `target/site/jacoco/index.html`, `target/site/jacoco/jacoco.xml`, and `target/site/jacoco/jacoco.csv`.

### Analysis
- Highest-risk uncovered paths are history pagination token repetition, partial stale-history recovery, duplicate history-event accounting/progress fallback, malformed Gmail watch/profile responses, and the currently unexecuted Pub/Sub-to-history listener orchestration seam.
- `RuleBasedEmailClassificationService` has 98.3% line but 85.3% branch coverage; the clearest specification-backed gaps are subject-side negation and the composite subject event rule (`film`/`dance`/`activity` plus time/location).
- `CategoryScoreService` has 100% line but 76.9% branch coverage; alternative programme/department conditions are not independently asserted.
- `GmailApiClient` has 94.7% line but 78.3% branch coverage; invalid watch/profile responses and nonblank history page-token serialization are untested.
- `GmailMessageMapper` has 92.3% line but 79.5% branch coverage; disposition-only attachments, malformed headers/senders, null parts, and invalid charset fallback remain characterization gaps.
- `GmailCommand` has 100% line but 85.7% branch coverage because successful retrieval exit status is not exercised.
- No production class is wholly uncovered. Fourteen compiled methods/lambdas are wholly uncovered, mainly application bootstrap, blocking Google subscriber/OAuth client assembly, passive entity getters, listener orchestration, and exception-supplier lambdas.
- Blocking subscriber lifecycle, browser OAuth, JVM bootstrap/`System.exit`, repository interfaces, and passive getters were classified as integration/manual, framework/bootstrap, or intentionally not unit-tested rather than targets for percentage-only tests.
- Existing tests contain meaningful behavioral assertions around persistence idempotency/concurrency, Gmail request details, history progress, safe logging, classifier category/confidence, API validation, and category-score components. JaCoCo measures execution, not correctness; repeated classifier and API plumbing paths contribute substantially to the global percentage.

### Files
- Maven: `pom.xml`.
- Documentation: new `docs/COVERAGE_LOG.md`; appended `docs/AGENT_PROMPTS.md` and `docs/WORK_LOG.md`.
- Production Java: unchanged.
- Test Java: unchanged.
- `docs/BUG_LOG.md`: unchanged because uncovered code is not automatically a defect.

### Git
Commit: Pending
PR: Pending
PR created by: Pending

### Next step
Add only three focused Gmail history safety integration tests: repeated history page token must throw without advancing state; partial stale-history recovery must throw without advancing or reading the new current history ID; and an already-persisted message on a page without an aggregate history ID must count as existing and advance to the notification ID. Run the focused realtime test, then `.\mvnw.cmd -B -ntp clean test`, and compare JaCoCo XML branch counters before selecting another batch.

---

## Work Entry
Date: 2026-10-06
Contributor: Codex
Milestone: CS5013 Code Coverage
Stage: Gmail History Safety Test Batch
Branch: phase4-live-demo

### Status
Completed — exactly three focused Gmail history synchronization tests were added and all existing production behavior was confirmed correct.

### Starting state
- Commit: `48b510514d8666db51bb29aaac008ed08c7eac0c`.
- Starting worktree: clean.
- Tests: 132 passing; 0 failures, 0 errors, 0 skipped.
- Overall coverage: 4,578/4,891 instructions (93.60%), 694/754 lines (92.04%), and 365/441 branches (82.77%).
- `GmailHistorySyncService`: 62/66 lines (93.94%) and 28/34 branches (82.35%).

### Test-first execution
- Added `repeatedHistoryPageTokenFailsWithoutAdvancingProgress`; first focused run passed: 8 tests, 0 failures, 0 errors, 0 skipped.
- Added `partialStaleHistoryRecoveryFailureDoesNotAdvanceProgress`; first focused run passed: 9 tests, 0 failures, 0 errors, 0 skipped.
- Added `duplicateMessageWithoutPageHistoryIdUsesNotificationProgress`; first focused run passed: 10 tests, 0 failures, 0 errors, 0 skipped.
- Focused command after each test: `.\mvnw.cmd -B -ntp "-Dtest=GmailRealtimeServiceTest" test`.
- The tests assert finite repeated-token failure, unchanged progress on both error paths, no profile history lookup after incomplete recovery, duplicate accounting without duplicate persistence, and notification-ID fallback when page history ID is absent.
- All three tests passed immediately. No genuine production defect was found, so no production file or `docs/BUG_LOG.md` was changed.

### Complete verification
- Command: `.\mvnw.cmd -B -ntp clean test`.
- Result: BUILD SUCCESS — **135 tests run, 0 failures, 0 errors, 0 skipped in 47.641 seconds**.
- JaCoCo report regenerated for all 45 production classes.

### Coverage result
- Overall instructions: 4,589/4,891 (**93.83%**), up from 93.60%.
- Overall lines: 697/754 (**92.44%**), up from 92.04%.
- Overall branches: 369/441 (**83.67%**), up from 82.77%.
- Overall methods/classes unchanged at 173/187 (92.51%) and 45/45 (100%).
- `GmailHistorySyncService` lines: 65/66 (**98.48%**), up from 62/66 (93.94%).
- `GmailHistorySyncService` branches: 32/34 (**94.12%**), up from 28/34 (82.35%).
- Newly covered outcomes: `created == false`; missing aggregate page history ID; repeated next-page token; and failed bounded stale-history recovery.
- Remaining history gaps: missing-watch-state exception line and blank-token short-circuit/termination branches, deliberately outside this three-test scope.

### Files
- Tests: `src/test/java/edu/campussignal/GmailRealtimeServiceTest.java`.
- Documentation: `docs/COVERAGE_LOG.md`, `docs/TEST_LOG.md`, `docs/AGENT_PROMPTS.md`, and `docs/WORK_LOG.md`.
- Production Java: unchanged.
- `docs/BUG_LOG.md`: unchanged.

### Git
Commit: Pending
PR: Pending
PR created by: Pending

### Next step
Create a separate small adapter/orchestration batch for nonblank Gmail history page-token serialization, invalid watch/profile responses, successful retrieve command exit status, and the captured `GmailListenerService` handler. Continue to require behavioral assertions and leave the JaCoCo gate unset.

---

## Work Entry
Date: 2026-10-07
Contributor: Codex
Milestone: Classifier Product Integration
Stage: Classifier → Ingestion → Persistence
Branch: phase4-live-demo

### Status
Completed — V1 classification is now part of the provider-neutral Java ingestion transaction for newly created emails.

### Pre-integration architecture discovered
- `EmailSource` lists provider message IDs and returns the provider-neutral `IncomingEmail` record.
- Gmail retrieval uses `GmailApiClient`/`GmailMessageMapper` to produce `IncomingEmail`; batch retrieval and Gmail history synchronization both call the same `EmailIngestionService`.
- `EmailIngestionService` first queries `EmailRepository.findByGmailMessageId`, then inserts in a `REQUIRES_NEW` `TransactionTemplate`; a unique `gmail_message_id` constraint and `DataIntegrityViolationException` recovery preserve concurrent idempotency.
- Before this change, `Email` persisted message metadata/body only, and `RuleBasedEmailClassificationService` was not registered as a Spring bean.

### Test-first implementation
- Added four Spring/H2 integration tests before production changes for new-email classification, duplicate skip/no reclassification, `GENERAL_OTHER`/`LOW`, classification failure, and direct use of `IncomingEmail` rather than Gmail API types.
- The first focused run failed during test compilation because the expected Email classification getters did not exist. Production implementation followed only after this RED result.
- `RuleBasedEmailClassificationService` is now the Spring `EmailClassificationService` bean and is constructor-injected into `EmailIngestionService`.
- For a new message, ingestion invokes `classifier.classify(incoming.subject(), incoming.body())` inside the existing insert transaction and constructs one classified `Email`; there is no second persistence path.

### Entity and schema decision
- Added `category`, persisted with `@Enumerated(EnumType.STRING)` so database values use the existing `EmailCategory` names.
- Added `classification_confidence`, represented in Java by the existing `ClassificationConfidence` enum and converted to the approved numeric values `0.9`, `0.7`, and `0.5` by `ClassificationConfidenceConverter`.
- Added the mappings to `ClassificationConfidence` itself rather than introducing a second confidence model.
- The two new columns are nullable at the schema level so Hibernate `ddl-auto=update` can add them to the existing file-backed H2 database without rewriting or silently reclassifying legacy rows. The `Email.classified` factory requires non-null category/confidence for every newly created entity.
- Existing direct-persistence test fixtures were updated only to supply explicit classification values required by the evolved entity.

### Duplicate and transaction behavior
- Ordinary duplicate lookup remains before classification. An existing row is returned unchanged with `created=false`; its content and classification are retained, and the classifier is not called.
- Concurrent insert races remain protected by the database uniqueness constraint. The losing transaction still resolves to the winning row with `created=false`, preserving retrieval/history `alreadyExisted` accounting.
- Classification and `saveAndFlush` execute inside the existing `REQUIRES_NEW` transaction. An unexpected classifier runtime exception is not swallowed by ingestion and leaves no email row. Existing retrieval policy still counts runtime failures as failed messages; history synchronization still cannot advance progress after such a failure.

### Verification
- New focused integration tests: 4 passed; 0 failures, 0 errors, 0 skipped.
- Existing persistence tests: 6 passed; 0 failures, 0 errors, 0 skipped.
- Gmail API/retrieval/history tests: 22 passed; 0 failures, 0 errors, 0 skipped.
- Existing classifier tests: 45 passed; 0 failures, 0 errors, 0 skipped.
- Complete suite: `.\mvnw.cmd -B -ntp clean test` — BUILD SUCCESS; 141 tests, 0 failures, 0 errors, 0 skipped in 49.891 seconds.
- SpotBugs: BUILD SUCCESS; 2 reviewed baseline findings, no new integration finding.
- PMD: BUILD SUCCESS; 27 baseline findings, no new integration-code violation.
- The initial integration-related SpotBugs constructor warning was corrected before completion; it was a static-analysis design issue introduced during implementation, not a pre-existing behavioral defect.

### Files changed
- Production: `src/main/java/edu/campussignal/dto/ClassificationConfidence.java`.
- Production: `src/main/java/edu/campussignal/entity/ClassificationConfidenceConverter.java`.
- Production: `src/main/java/edu/campussignal/entity/Email.java`.
- Production: `src/main/java/edu/campussignal/service/EmailIngestionService.java`.
- Production: `src/main/java/edu/campussignal/service/RuleBasedEmailClassificationService.java`.
- Tests: new `src/test/java/edu/campussignal/EmailClassificationIngestionTest.java`.
- Test fixtures: `src/test/java/edu/campussignal/EmailPersistenceTest.java` and `src/test/java/edu/campussignal/GmailRealtimeServiceTest.java`.
- Documentation: `docs/AGENT_PROMPTS.md`, `docs/TEST_LOG.md`, and `docs/WORK_LOG.md`.
- `docs/BUG_LOG.md`: unchanged because no genuine product defect was discovered.

### Git
Commit: Pending
PR: Pending
PR created by: Pending

### Next step
Design an explicit one-time backfill/migration for legacy emails whose newly added classification columns are null, separate from ordinary idempotent ingestion; then expose stored classification through a read-side service/API before beginning personalized ranking.

---

## Work Entry
Date: 2026-10-07
Contributor: Codex
Milestone: Legacy Classification Migration
Stage: Explicit Email Classification Backfill
Branch: phase4-live-demo

### Status
Completed — legacy and partially classified email rows can now be classified by an explicit,
idempotent, provider-neutral maintenance command without entering ordinary ingestion.

### Existing legacy handling discovered
- `Email.category` and `Email.classificationConfidence` were intentionally nullable so file-backed H2 rows created before classifier integration remained readable.
- New mail already used `Email.classified(...)` inside the ingestion insert transaction, while duplicate ingestion returned the existing row without classification.
- No query, service, command, or automatic migration existed for incomplete legacy classification.
- The existing explicit operational boundary was a property-gated Spring `ApplicationRunner` used for Gmail commands, with command-mode process exit in `CampusSignalApplication`.

### Architecture and eligibility
- Added a dedicated `EmailClassificationBackfillService`; it is not called by ingestion, retrieval, history synchronization, controllers, or normal startup.
- Added a repository query selecting rows ordered by ID where `category is null OR classificationConfidence is null`.
- Either partial-state direction is eligible. The existing V1 classifier receives only the stored subject/body, then both fields are replaced together with the returned category/confidence pair. This deterministically repairs inconsistent legacy pairs.
- Fully classified rows are absent from the query, are not sent to the classifier, and are never treated as candidates for classifier-version migration.
- The result reports the number updated. It does not create a second persistence path or insert any rows.

### Transaction, failure, and idempotency policy
- One `@Transactional` service call covers the complete selected batch. JPA dirty checking plus an explicit flush persists updates only after classification results have been applied.
- A classifier or persistence exception propagates. The whole run rolls back, including updates prepared for earlier rows; there is no silent per-row failure or partially updated batch.
- After a successful run, no eligible rows remain. A rerun returns `updated=0`, performs no classification calls, preserves IDs/data, and creates no duplicates.

### Explicit invocation
- Added `EmailClassificationBackfillCommand`, active only when `--classification.backfill=true` is supplied.
- Invoke after packaging with `java -jar target/campussignal-0.1.0-SNAPSHOT.jar --spring.main.web-application-type=none --classification.backfill=true`.
- On success it prints `classificationBackfillUpdated=<count>` and exits. An uncaught migration failure makes Spring Boot startup fail and returns a nonzero process result.
- A packaged smoke against an isolated in-memory H2 database exited 0 and reported zero updates. No Gmail client, API object, provider fetch, or ingestion service participates.

### Test-first and verification
- Initial RED: the focused test command failed compilation with six missing-symbol errors for the deliberately absent backfill service/command.
- Six new focused tests now pass. An intermediate assertion exposed only H2 nanosecond-to-microsecond storage precision in the test fixture; comparing database values before/after corrected the test without production changes.
- Focused backfill: 6 passed; existing persistence: 6 passed; classifier: 45 passed; classification ingestion: 4 passed; Gmail API/command/retrieval/history: 29 passed.
- Complete suite: `.\mvnw.cmd -B -ntp clean test` — BUILD SUCCESS; **147 tests, 0 failures, 0 errors, 0 skipped** in 49.496 seconds.
- SpotBugs: BUILD SUCCESS; 2 unchanged reviewed baseline findings; no backfill finding.
- PMD: BUILD SUCCESS; 27 unchanged reviewed baseline findings; no backfill-code finding.
- No genuine product defect was discovered. Classifier V1 rules, Gmail behavior, duplicate handling, nullable schema compatibility, and ordinary ingestion semantics remain unchanged.

### Files changed
- Production: `src/main/java/edu/campussignal/CampusSignalApplication.java`.
- Production: `src/main/java/edu/campussignal/command/EmailClassificationBackfillCommand.java` (new).
- Production: `src/main/java/edu/campussignal/entity/Email.java`.
- Production: `src/main/java/edu/campussignal/repository/EmailRepository.java`.
- Production: `src/main/java/edu/campussignal/service/EmailClassificationBackfillService.java` (new).
- Tests: `src/test/java/edu/campussignal/EmailClassificationBackfillServiceTest.java` (new).
- Tests: `src/test/java/edu/campussignal/command/EmailClassificationBackfillCommandTest.java` (new).
- Documentation: `docs/AGENT_PROMPTS.md`, `docs/JAVA_BACKEND.md`, `docs/TEST_LOG.md`, and `docs/WORK_LOG.md`.
- `docs/BUG_LOG.md`: unchanged because no genuine behavioral defect was found.
- Pre-existing worktree edits in `EmailPersistenceTest` and `GmailRealtimeServiceTest` were preserved and not changed by this task.

### Git
Commit: Pending
PR: Pending
PR created by: Pending

### Next step
Run the explicit backfill once against a backed-up copy of the intended file-backed H2 database,
verify the reported count and remaining-null query, then begin a separate read-side classification
API/service task. Do not begin personalized ranking until stored classification is exposed and
validated through that read path.

---
