## Prompt Entry
Date: 2026-09-08
Contributor: Priyanshi Garg
Branch: feature/user-profile
Milestone: 11 September 2026 — Email Retrieval and User Preference Setup
Task: User Profile Foundation

### Prompt
Work on the CampusSignal repository currently checked out locally.

Current milestone:
11 September 2026 — Email Retrieval and User Preference Setup.

For this task, implement ONLY the User Profile foundation. Do not implement classification, AI/NLP, embeddings, ranking, frontend, or Gmail changes.

Before changing code:
1\. Inspect the existing repository structure, models, schemas, database initialization, tests, and coding conventions.
2\. Confirm the current branch. Do not work directly on main.
3\. If not already on a feature branch, create:
&#x20;  feature/user-profile
4\. Run the existing test suite before making changes and record the baseline result.

IMPLEMENTATION

Create a UserProfile database model following the repository's existing SQLAlchemy conventions.

The profile must support:
\- id
\- email
\- department
\- programme
\- year
\- semester (optional)
\- interests
\- created\_at

Requirements:
\- email must be unique and non-null.
\- department must be non-null.
\- programme must be non-null.
\- year must be non-null.
\- semester may be null.
\- interests must contain a list of strings.
\- created\_at must be generated automatically.
\- Use the existing Base/database configuration.
\- Register the model wherever models must be imported so that the table is created by the current database initialization mechanism.
\- Do not introduce a new ORM, migration framework, database, or architecture.

Create Pydantic schemas following existing project conventions.

Required schemas:
\- UserProfileCreate
\- UserProfileRead

Validation requirements:
\- reject empty department
\- reject empty programme
\- year must be >= 1
\- semester, when provided, must be >= 1
\- require at least one interest
\- trim interest whitespace
\- normalize interests to lowercase
\- remove duplicate interests while preserving order
\- reject unexpected extra fields if consistent with the project's current schema conventions

Do NOT restrict interests to a fixed enum yet. Interests must remain extensible.

Suggested files, if consistent with the existing repository:
\- app/models/user\_profile.py
\- app/schemas/user\_profile.py

Update database model registration only where required by the current architecture.

TESTS

Add focused tests covering at least:
1\. valid profile schema creation
2\. interest lowercase normalization
3\. whitespace removal
4\. duplicate interest removal
5\. invalid empty interests
6\. invalid year
7\. invalid semester
8\. UserProfile table creation/registration
9\. existing project tests continue to pass

Use the repository's existing test fixtures and testing style. Do not create unnecessary duplicate infrastructure.

Run:
python -m pytest tests/test\_user\_profile.py -v
and then:
python -m pytest

Do not finish the task with failing tests.

DOCUMENTATION / PROJECT TRACEABILITY

Create a docs directory if one does not already exist.

Create these two files:

1\. docs/AGENT\_PROMPTS.md
2\. docs/WORK\_LOG.md

Also update the repository's AGENTS.md, if it exists, with a short persistent section named:

\## Project Traceability Rules

Add these rules:
\- Every coding-agent task must append the exact task prompt to docs/AGENT\_PROMPTS.md before or during implementation.
\- Every completed development task must append an entry to docs/WORK\_LOG.md.
\- Never overwrite previous entries.
\- Never invent commit hashes, PR numbers, PR URLs, authors, dates, or test results.
\- If a PR has not yet been created, record its status as "Pending".
\- Documentation updates are part of the definition of done.

AGENT\_PROMPTS.md FORMAT

For this task append:

\## Prompt Entry
Date: \<actual date>
Contributor: Priyanshi Garg
Branch: \<actual current branch>
Milestone: 11 September 2026 — Email Retrieval and User Preference Setup
Task: User Profile Foundation

\### Prompt
Paste the complete prompt received for this task verbatim here.

\---

For every future agent task, append another entry. Never modify or delete older prompts.

WORK\_LOG.md FORMAT

Append an entry using exactly this structure:

\## Work Entry
Date: \<actual date>
Contributor: Priyanshi Garg
Milestone: 11 September 2026 — Email Retrieval and User Preference Setup
Stage: User Profile Foundation
Branch: \<actual branch>

\### Status
Completed / In Progress / Blocked

\### Work completed
\- concise list of actual changes

\### Files changed
\- actual files only

\### Tests
\- baseline test result
\- new focused test result
\- final complete test result

\### Git
Commit: \<actual commit hash, or Pending>
PR: \<actual PR number/URL, or Pending>
PR created by: \<actual person, or Pending>

\### Next stage
User Profile API/service persistence, followed by Initial Category Preference Score implementation.

\---

Do not claim that a PR exists unless one actually exists.

SCOPE RESTRICTIONS

Do not modify:
\- Gmail OAuth behavior
\- Gmail fetching/sync
\- Gmail watch
\- Pub/Sub
\- Gmail History API logic
unless a change is strictly required to fix an import caused by this feature, in which case explain it first.

Do not implement:
\- email classification
\- summaries
\- deadline extraction
\- eligibility extraction
\- embeddings
\- semantic matching
\- final relevance ranking
\- feedback learning
\- frontend

Do not refactor unrelated code.

Do not migrate the project to another language/framework as part of this task.

AT THE END

Show me:
1\. branch used
2\. files created
3\. files modified
4\. concise explanation of the UserProfile design
5\. baseline test count
6\. focused test result
7\. final full test result
8\. git diff --stat
9\. git status
10\. whether a commit was created
11\. whether a PR was created

If you encounter an architectural conflict with the existing repository, stop and explain the conflict instead of inventing a new architecture.

---

## 2026-10-07 — Final classifier coverage batch

### Exact task prompt

> We have completed the first JaCoCo coverage-improvement batch.
>
> Current verified state:
>
> - Full Java suite: 135 tests
> - 0 failures
> - 0 errors
> - 0 skipped
> - Overall line coverage: 92.44%
> - Overall branch coverage: 83.67%
> - GmailHistorySyncService branch coverage: 94.12%
> - No production code changed in the previous coverage batch
> - No bugs were discovered
>
> This task is the FINAL small classifier coverage batch before stopping coverage-driven work and moving to classifier integration.
>
> Do not add a broad set of new tests.
>
> Add only the two meaningful classifier scenarios identified in the JaCoCo baseline.
>
> ## Test 1 — subject-side negation
>
> Add a focused test proving that a specialist category cue appearing in the SUBJECT within the existing four-token negation window is not counted positively.
>
> Use the existing Classification Specification V1 behavior.
>
> Examples should reflect the existing negation semantics rather than inventing new NLP behavior.
>
> Verify:
>
> - the negated subject cue is suppressed;
> - another valid current-message signal can still determine the category if present;
> - confidence follows the existing V1 rules;
> - the test does not alter the existing four-token negation contract.
>
> Do not change production code unless the test demonstrates a genuine defect.
>
> ## Test 2 — composite subject event rule
>
> Add the focused test corresponding to the currently uncovered composite subject-event branch identified in the JaCoCo report.
>
> Before writing the test:
>
> 1. inspect the exact uncovered branch in RuleBasedEmailClassificationService / ClassificationTextPreprocessor;
> 2. identify the existing intended rule from CLASSIFICATION_SPEC_V1.md;
> 3. write the smallest test that exercises that currently uncovered outcome.
>
> Do not invent a new classification rule merely to obtain branch coverage.
>
> The expected behavior must follow the already approved specification.
>
> ## Test-first process
>
> For each test:
>
> 1. inspect current production code and existing tests;
> 2. add the focused test;
> 3. run RuleBasedEmailClassificationServiceTest;
> 4. record whether the test passes immediately or exposes a defect;
> 5. modify production code only if existing behavior contradicts the approved V1 specification.
>
> Do not change, weaken, rename, disable, or skip any of the existing 43 classifier tests.
>
> ## Verification
>
> After both tests:
>
> Run the classifier suite.
>
> Then run:
>
> `.\mvnw.cmd -B -ntp clean test`
>
> Record:
>
> - classifier test count;
> - total Java test count;
> - failures;
> - errors;
> - skipped;
> - BUILD result.
>
> Inspect JaCoCo and compare:
>
> - overall branch coverage before vs after;
> - RuleBasedEmailClassificationService branch coverage before vs after;
> - ClassificationTextPreprocessor branch coverage before vs after;
> - exact newly covered branches.
>
> Do not add a coverage gate.
>
> ## Documentation
>
> Update:
>
> - `docs/COVERAGE_LOG.md`
> - `docs/TEST_LOG.md`
> - `docs/AGENT_PROMPTS.md`
> - `docs/WORK_LOG.md`
>
> Update `BUG_LOG.md` only if a genuine behavioral defect is discovered.
>
> ## Restrictions
>
> Do NOT:
>
> - add more Gmail-history tests;
> - add the proposed adapter/orchestration coverage batch yet;
> - add tests merely to increase the overall percentage;
> - modify classifier evidence lists;
> - modify precedence;
> - modify confidence mapping;
> - modify persistence;
> - implement ranking;
> - change database schema;
> - modify PMD or SpotBugs;
> - add a JaCoCo coverage threshold;
> - commit or push automatically.
>
> ## Completion report
>
> Report:
>
> 1. Starting classifier and total test counts.
> 2. Tests added.
> 3. Whether each passed immediately.
> 4. Any defects discovered.
> 5. Production files changed, if any.
> 6. Classifier-focused result.
> 7. Complete Java-suite result.
> 8. Overall branch coverage before vs after.
> 9. Classifier/preprocessor branch coverage before vs after.
> 10. Exact branches newly covered.
> 11. Files changed.
> 12. Exact next step.
>
> After these two tests, recommend moving away from coverage-driven work and beginning classifier integration with email ingestion/persistence unless a genuine defect is exposed.

### Prompt summary

Add only the two specification-backed classifier tests for subject-side negation and the composite subject event rule, run focused verification after each and then the complete suite, compare exact JaCoCo branch coverage, change production only for a demonstrated defect, update the required traceability logs, and then stop coverage-driven work in favor of classifier integration.

---

## 2026-10-06 — CS5013 JaCoCo baseline analysis and triage

### Exact task prompt

> We have completed the CampusSignal PMD analysis and remediation phase.
>
> Current verified state:
>
> - Branch: `phase4-live-demo`
> - Complete Java suite: 132 tests
> - Failures: 0
> - Errors: 0
> - Skipped: 0
> - PMD findings reduced from 32 to 27
> - Major genuine complexity findings in GmailApiClient, GmailNotificationDecoder, and GmailHistorySyncService were refactored
> - RuleBasedEmailClassificationService preprocessing was extracted into ClassificationTextPreprocessor
> - All externally observable Gmail and classifier behavior was preserved
> - No JaCoCo configuration exists yet unless inspection proves otherwise
>
> We are now beginning the CS5013 CODE COVERAGE phase using JaCoCo.
>
> This task is ANALYSIS FIRST.
>
> Do NOT immediately add tests simply to increase a percentage.
>
> ## 1. Establish clean baseline
>
> Before changing tooling:
>
> - record branch
> - record commit SHA
> - confirm working tree is clean
> - run:
>
> `.\mvnw.cmd -B -ntp clean test`
>
> Record the actual test count and result.
>
> Expected current result:
>
> 132 tests
> 0 failures
> 0 errors
> 0 skipped
>
> Use the actual result if it differs.
>
> ## 2. Inspect Maven configuration
>
> Read `pom.xml`.
>
> Add an appropriate JaCoCo Maven plugin configuration compatible with the existing Java/Spring Boot/Maven project.
>
> Use Maven dependency/plugin management.
>
> Do not manually download a JaCoCo executable.
>
> Do not remove or alter SpotBugs or PMD configuration.
>
> Configure JaCoCo so the normal test execution generates a coverage report.
>
> Prefer generation of:
>
> - HTML report
> - XML report
> - CSV report if useful
>
> Do NOT initially introduce a mandatory minimum coverage gate.
>
> We first want to observe the real baseline.
>
> ## 3. Run the complete test suite with JaCoCo
>
> Run the complete Java test suite under JaCoCo.
>
> Generate the report.
>
> Record project-level:
>
> - instruction coverage
> - line coverage
> - branch coverage
> - method coverage
> - class coverage
>
> Where available, record both covered and missed counts in addition to percentages.
>
> ## 4. Analyze coverage by package and class
>
> Do not focus only on the global percentage.
>
> Identify:
>
> - lowest-covered production classes
> - classes with high line coverage but poor branch coverage
> - important business logic with uncovered branches
> - completely uncovered production methods/classes
> - failure/error-handling paths that are untested
> - infrastructure code that is difficult or inappropriate to unit-test
>
> Pay particular attention to:
>
> - RuleBasedEmailClassificationService
> - ClassificationTextPreprocessor
> - GmailApiClient
> - GmailNotificationDecoder
> - GmailHistorySyncService
> - GmailCommand
> - PubSubPullSubscriber
> - Email ingestion/persistence services
> - Profile/category scoring logic
>
> Do not assume every class must have identical coverage.
>
> ## 5. Interpret gaps rather than chasing numbers
>
> For each meaningful uncovered region classify it as:
>
> - IMPORTANT BEHAVIORAL GAP
> - FAILURE-PATH GAP
> - LOW-RISK INFRASTRUCTURE GAP
> - GENERATED/FRAMEWORK/BOOTSTRAP CODE
> - INTENTIONALLY NOT UNIT-TESTED
> - NEEDS INTEGRATION TEST RATHER THAN UNIT TEST
>
> For every important gap explain:
>
> 1. class/method
> 2. uncovered lines or branches
> 3. what scenario would execute them
> 4. whether that scenario matters to CampusSignal
> 5. what type of test would be appropriate
> 6. whether a test should be added now or deferred
>
> Do not write meaningless tests that merely execute code without assertions.
>
> ## 6. Branch coverage is especially important
>
> Pay particular attention to conditional logic where line coverage may hide missing paths.
>
> Examples include:
>
> - classifier precedence
> - confidence selection
> - negation handling
> - null/malformed Gmail data
> - pagination
> - history synchronization
> - exception handling
> - fallback behavior
>
> If a condition has two or more outcomes, determine which outcomes are exercised.
>
> Do not confuse high line coverage with complete behavioral verification.
>
> ## 7. Existing test quality
>
> Relate coverage results to the existing 132 tests.
>
> Identify examples where:
>
> - existing tests provide meaningful assertions
> - multiple tests hit the same code path
> - an uncovered branch represents an actual missing scenario
> - coverage is high but test quality could still be weak
>
> Explicitly record that JaCoCo measures execution, not correctness.
>
> ## 8. Do not add tests yet unless required for tooling verification
>
> This first task is baseline analysis.
>
> Do NOT begin a broad coverage-improvement campaign during the initial run.
>
> We want the untouched baseline preserved.
>
> After analysis, recommend a small prioritized coverage batch.
>
> ## 9. Documentation
>
> Create:
>
> `docs/COVERAGE_LOG.md`
>
> Record:
>
> - date
> - branch
> - commit
> - Java version
> - JaCoCo plugin version
> - Maven command
> - test count
> - project-level coverage
> - package/class coverage
> - meaningful uncovered branches
> - triage of coverage gaps
> - recommended next tests
>
> Append a concise task summary to:
>
> `docs/AGENT_PROMPTS.md`
>
> Append execution details to:
>
> `docs/WORK_LOG.md`
>
> Do not update `BUG_LOG.md` simply because code is uncovered.
>
> An uncovered branch is not automatically a bug.
>
> ## 10. Restrictions
>
> Do NOT:
>
> - modify classifier behavior
> - alter Gmail behavior
> - refactor production code
> - modify database schema
> - implement ranking
> - add coverage thresholds yet
> - add arbitrary tests solely to increase percentages
> - remove SpotBugs or PMD
> - delete Python
> - commit or push automatically
>
> ## 11. Completion report
>
> Report:
>
> 1. Branch and commit.
> 2. Baseline test result.
> 3. JaCoCo plugin version.
> 4. Maven configuration added.
> 5. Command used.
> 6. Overall line coverage.
> 7. Overall branch coverage.
> 8. Method/class coverage.
> 9. Highest-risk uncovered production areas.
> 10. Classes with misleadingly high line but low branch coverage.
> 11. Uncovered areas that do not warrant unit tests.
> 12. Recommended prioritized test additions.
> 13. Files changed.
> 14. Post-tooling full-suite result.
> 15. Confirmation that production behavior was untouched.
> 16. Exact recommended next step.
>
> Stop after initial coverage analysis and triage.

### Prompt summary

Establish and preserve the clean 132-test Java baseline; add JaCoCo to the normal Maven test lifecycle without changing PMD, SpotBugs, production behavior, or coverage thresholds; generate HTML/XML/CSV reports; analyze instruction, line, branch, method, class, package, and risk-focused class coverage; distinguish meaningful behavioral and failure gaps from infrastructure/bootstrap code; assess the quality and overlap of the existing tests; document the baseline and triage; recommend a small prioritized test batch; and stop before adding coverage-improvement tests, committing, or pushing.

---

## 2026-10-06 — PMD test-first maintainability remediation

### Exact task prompt

> We have completed the initial PMD analysis and triage for CampusSignal.
>
> Current verified state:
>
> - Branch: `phase4-live-demo`
> - Complete Java suite: 122 tests, 0 failures, 0 errors, 0 skipped
> - Maven PMD Plugin: 3.28.0
> - PMD runtime: 7.27.0
> - Initial PMD findings: 32 across 13 classes
> - 5 findings were triaged as genuine maintainability concerns
> - 8 as minor style issues
> - 17 as false positives/not applicable
> - 2 require further investigation
> - No production/test Java code has yet been changed due to PMD
>
> The source of truth for the triage is:
>
> `docs/PMD_ANALYSIS_LOG.md`
>
> This task is TEST-FIRST, BEHAVIOUR-PRESERVING REFACTORING.
>
> Do not attempt to make the PMD report globally clean.
>
> Do not modify code merely to reduce a metric.
>
> ## 1. Preserve baseline
>
> Before production changes:
>
> - record branch
> - record current commit
> - confirm working tree is clean
> - run:
>
> `.\mvnw.cmd -B -ntp clean test`
>
> Record the actual result.
>
> Expected current baseline:
>
> 122 tests
> 0 failures
> 0 errors
> 0 skipped
>
> Then run PMD and preserve the current finding/metric baseline.
>
> ## 2. Refactoring priorities
>
> Work on the confirmed maintainability findings in this order unless source inspection reveals a safety reason to change the order.
>
> ### A. GmailApiClient.listMessageIds
>
> Current PMD signal:
>
> - Cognitive Complexity: 21
> - Cyclomatic Complexity: 15
>
> Before refactoring:
>
> 1. Read the method and all existing GmailApiClient tests.
> 2. Identify every distinct observable behaviour:
>    - successful page retrieval
>    - pagination
>    - empty results
>    - null/missing message lists where relevant
>    - next-page-token handling
>    - API failure propagation
>    - maximum-result behavior
> 3. Determine which behaviours already have tests.
> 4. Add only missing characterization tests necessary to protect the current behavior.
>
> Then refactor the method into smaller cohesive helpers where justified.
>
> Possible responsibilities to separate may include:
>
> - fetching one page
> - extracting IDs
> - pagination-state handling
> - result-limit handling
>
> Do not change Gmail API semantics.
>
> Do not add extra network calls.
>
> Do not alter public behavior merely to satisfy PMD.
>
> After the refactor:
>
> - run focused GmailApiClient tests
> - run complete suite
> - rerun PMD
> - record complexity before vs after
>
> ### B. GmailNotificationDecoder.decode
>
> Current PMD signal:
>
> - Cyclomatic Complexity: 18
>
> Before refactoring:
>
> 1. Inspect existing decoder tests.
> 2. Characterize:
>    - valid notification
>    - malformed Base64
>    - malformed JSON
>    - missing fields
>    - null/blank data if supported
>    - valid history ID / email-address behavior
> 3. Add missing tests only where behavior is currently unprotected.
>
> Then separate parsing/validation steps where doing so reduces branching and improves readability.
>
> Do not weaken validation.
>
> Do not change accepted/rejected notification formats unless an actual bug is discovered.
>
> Verify focused tests → full suite → PMD.
>
> ### C. GmailHistorySyncService.messageAddedIds
>
> Current PMD signal:
>
> - Cognitive Complexity: 16
>
> Characterize existing behavior before refactoring:
>
> - empty history
> - history entries with no messages-added data
> - valid message-added IDs
> - duplicate IDs if relevant
> - null/missing nested Google API structures where currently supported
>
> Prefer extraction of small helpers for nested traversal/null handling if that improves clarity.
>
> Do not change history-sync semantics.
>
> Verify focused tests → full suite → PMD.
>
> ## 3. RuleBasedEmailClassificationService
>
> PMD reported:
>
> `TooManyMethods`
>
> The earlier `GodClass` finding was explicitly triaged as NOT APPLICABLE because this class has one cohesive classification responsibility.
>
> Do not split the classifier arbitrarily merely to reduce the method count.
>
> Inspect whether there is one cohesive sub-responsibility suitable for extraction.
>
> The preferred candidate is:
>
> - classification text preprocessing
> - normalisation
> - quote segmentation
> - signature/footer/URL filtering
> - nearby-negation handling
>
> If extraction is justified, create a small package-private collaborator dedicated to text preprocessing.
>
> Before extraction, add focused characterization tests only for currently under-protected preprocessing behavior, such as:
>
> - repeated `Re:`/`Fwd:` prefixes
> - Unicode whitespace/punctuation normalization
> - quote boundary handling
> - four-token negation-window boundary
> - text filtering behavior
>
> Do NOT change:
>
> - category evidence lists
> - evidence scores
> - primary-purpose precedence
> - confidence mapping
> - classification expectations
>
> All existing 40 classifier tests must remain unchanged and passing.
>
> The refactoring should be behavior-preserving.
>
> ## 4. GmailMessageMapper.collectText
>
> PMD reported cyclomatic complexity 12.
>
> This item is currently marked for further investigation.
>
> Do NOT refactor it immediately.
>
> First inspect:
>
> - current MIME-mapping behavior
> - current tests
> - nested multipart behavior
> - text/plain vs text/html preference
> - malformed/empty parts
> - charset handling
>
> Decide whether the complexity represents genuine maintainability risk or is inherent, readable MIME traversal.
>
> If existing characterization is insufficient, recommend tests.
>
> Do not refactor this method in this task unless the evidence clearly supports doing so and its behavior is adequately protected.
>
> ## 5. Stale-history exception cause preservation
>
> This is also marked for further investigation.
>
> Review the privacy/error-boundary implications.
>
> Determine whether preserving an underlying exception cause would:
>
> - improve diagnosability
> - expose private Gmail/OAuth information
> - alter externally visible behavior
>
> Do not change it merely because PMD flagged it.
>
> Document the decision separately.
>
> ## 6. Minor style and false-positive findings
>
> Do not spend this task chasing:
>
> - the 17 findings already triaged false-positive/not-applicable
> - cosmetic issues with no meaningful maintainability impact
> - Google API `LooseCoupling` findings where concrete library model types are required
> - intentional framework patterns
>
> Do not add blanket suppressions.
>
> ## 7. Refactoring discipline
>
> For every real refactor:
>
> 1. tests GREEN before change
> 2. characterize missing behavior if necessary
> 3. make one structural change
> 4. focused tests
> 5. complete regression suite
> 6. PMD rerun
> 7. compare metric/findings before vs after
>
> Never combine several major refactors before verification.
>
> Do not change code solely because a metric number is high.
>
> ## 8. Documentation
>
> Update:
>
> `docs/PMD_ANALYSIS_LOG.md`
>
> For each remediated finding record:
>
> - PMD rule/metric
> - before value
> - characterization tests
> - refactoring performed
> - after value
> - behavioral verification
> - whether finding disappeared/remained
> - justification
>
> Update:
>
> `docs/TEST_LOG.md`
>
> with any new characterization tests.
>
> Append a concise summary to:
>
> `docs/AGENT_PROMPTS.md`
>
> Append execution history to:
>
> `docs/WORK_LOG.md`
>
> Update:
>
> `docs/BUG_LOG.md`
>
> only if investigation reveals an actual behavioural defect. Complexity alone is not a bug.
>
> ## 9. Restrictions
>
> Do NOT:
>
> - change classifier specification
> - change Gmail externally observable behavior
> - alter database schema
> - modify profile scoring
> - implement ranking
> - add JaCoCo yet
> - add performance profiling yet
> - delete Python
> - chase all PMD warnings
> - add broad PMD suppressions
> - commit or push automatically
>
> ## 10. Completion report
>
> Report:
>
> 1. Baseline test result.
> 2. PMD baseline.
> 3. Characterization tests added.
> 4. Production files refactored.
> 5. Complexity metrics before vs after.
> 6. Focused test results.
> 7. Complete Java-suite result.
> 8. PMD findings before vs after.
> 9. Findings deliberately left unchanged and why.
> 10. Any genuine behavioural bugs discovered.
> 11. Confirmation that externally observable behavior was preserved.
> 12. Exact next recommended step.
>
> Stop after the approved PMD remediation/refactoring phase.

### Prompt summary

Asked Codex to preserve the clean 122-test and 32-finding PMD baselines; add only missing characterization tests; sequentially refactor the confirmed Gmail API pagination, notification decoding, history traversal, and classifier preprocessing maintainability findings with focused/full-suite/PMD verification after each; investigate but not automatically change MIME traversal or stale-history cause preservation; leave style and false-positive findings alone; document exact before/after metrics and verification; and stop without committing or pushing.

---

## Prompt Entry
Date: 2026-10-06
Contributor: Codex
Branch: phase4-live-demo
Milestone: CS5013 Static Analysis
Task: Test-First SpotBugs Remediation

### Exact task prompt

> We have completed the initial SpotBugs analysis on CampusSignal.
>
> Verified state:
>
> - Branch: `phase4-live-demo`
> - Baseline Java suite: 117 tests, 0 failures, 0 errors, 0 skipped
> - SpotBugs Maven plugin: 4.10.4.1
> - SpotBugs engine: 4.10.4
> - Initial findings: 6
> - Confirmed runtime defects: none
> - Production Java code has not yet been changed because of SpotBugs
>
> The initial triage is documented in:
>
> `docs/STATIC_ANALYSIS_LOG.md`
>
> This task is a TEST-FIRST remediation of the justified maintainability findings.
>
> Do NOT attempt to make SpotBugs report zero findings by blindly modifying code or adding blanket suppressions.
>
> ### Approved policy for Gmail command exception boundary
>
> The broad outer Gmail-command exception boundary may remain as a user-facing/privacy boundary, but it must not silently hide unexpected programming failures.
>
> Expected operational failures may produce sanitized user-facing error messages.
>
> Unexpected programming/runtime failures must remain diagnosable through appropriate internal logging/error propagation while still avoiding exposure of credentials, tokens, private mailbox content, or sensitive exception details to the user.
>
> Do not expose secrets merely to satisfy static analysis.
>
> ### Finding group 1 — history-ID parsing
>
> SpotBugs reported `DCN_NULLPOINTER_EXCEPTION` in two history-ID parsing paths.
>
> Inspect both locations.
>
> The objective is to remove `NullPointerException` as normal control flow.
>
> Before changing production code:
>
> 1. Identify the two parser methods/paths.
> 2. Identify current inputs that can produce null/missing history IDs.
> 3. Write focused JUnit tests for:
>    - valid history ID;
>    - missing/null history ID;
>    - malformed history ID if relevant to current contract;
>    - existing expected fallback/failure behaviour.
>
> Then refactor the parsing logic so null is handled explicitly rather than by catching `NullPointerException`.
>
> Preserve existing observable behaviour unless the documented contract requires correction.
>
> Use explicit null checks, Optional-style handling, or another simple Java construct appropriate to the existing codebase.
>
> Do not over-engineer.
>
> ### Finding group 2 — Pub/Sub broad `throws Exception`
>
> SpotBugs/triage identified a broad `throws Exception` contract in the Pub/Sub handler path.
>
> Inspect the interface and its implementations/callers.
>
> Before modifying the signature:
>
> 1. Determine which checked exceptions can actually occur.
> 2. Determine whether those exceptions are implementation details that should be wrapped.
> 3. Write focused tests for the existing success and failure behaviour.
>
> Prefer one of the following, depending on the actual code:
>
> - a specific checked exception;
> - a project-specific exception if several implementation-specific checked exceptions must be abstracted;
> - an unchecked exception only if that matches the architectural boundary and existing error handling.
>
> Do NOT simply replace `throws Exception` with `RuntimeException` without reasoning.
>
> The goal is a more precise and understandable contract.
>
> ### Finding group 3 — Gmail command exception boundary
>
> Do not remove the privacy/error boundary merely because SpotBugs questioned the broad catch.
>
> Inspect the current command code and tests.
>
> Verify through tests that:
>
> - expected Gmail/OAuth/PubSub operational failures result in sanitized user-visible output;
> - sensitive exception messages are not leaked;
> - unexpected runtime/programming failures are still diagnosable and do not look like successful execution.
>
> If existing tests already prove these behaviours, record that.
>
> If a small test is missing, add the minimum focused test.
>
> Change production code only if the approved policy is not currently satisfied.
>
> ### False positives
>
> The two currently triaged false positives are:
>
> - unmodifiable `ProfileCreateRequest.interests()`
> - trusted Spring-injected `ObjectMapper`
>
> Do NOT change correct production behaviour merely to silence these findings.
>
> Do NOT add blanket SpotBugs exclusions.
>
> Leave them documented for a later narrow-suppression decision.
>
> ### Verification order
>
> After each remediation group:
>
> 1. run the focused tests;
> 2. run the relevant existing regression tests;
> 3. only then proceed to the next group.
>
> When production changes are complete:
>
> 1. run all 117+ Java tests;
> 2. run SpotBugs again;
> 3. compare the new report against the initial six-finding baseline.
>
> Record the actual test count because new focused tests may increase the total above 117.
>
> ### Documentation
>
> Update `docs/STATIC_ANALYSIS_LOG.md` with:
>
> - original finding;
> - triage decision;
> - test added;
> - root cause;
> - production change;
> - post-fix SpotBugs status;
> - whether the finding disappeared or remains;
> - rationale for any unresolved finding.
>
> Update `docs/TEST_LOG.md` with new tests and results.
>
> Append a concise task summary to `docs/AGENT_PROMPTS.md`.
>
> Append the full execution record to `docs/WORK_LOG.md`.
>
> Update `docs/BUG_LOG.md` only if investigation reveals a genuine behavioural defect. Maintainability findings alone are not automatically bugs.
>
> ### Restrictions
>
> Do not modify classifier rules, ranking, Gmail ingestion behaviour, database schema, profile scoring, or unrelated modules.
>
> Do not add PMD or JaCoCo yet.
>
> Do not delete Python.
>
> Do not add broad SpotBugs suppressions.
>
> Do not commit or push automatically.
>
> ### Completion report
>
> Report:
>
> - findings remediated;
> - tests added;
> - production files changed;
> - whether Gmail exception-boundary behaviour changed;
> - focused test results;
> - complete Java-suite result;
> - SpotBugs findings before vs after;
> - remaining findings and their triage state;
> - whether any genuine new bugs were discovered;
> - exact next recommended step.

---

## Prompt Entry
Date: 2026-09-08
Contributor: Priyanshi Garg
Branch: feature/java-springboot-migration
Milestone: 11 September 2026 — Email Retrieval and User Preference Setup
Task: Java/Spring Boot Migration

### Prompt
# Context from my IDE setup:

## Active file: docs/WORK_LOG.md

## Open tabs:
- WORK_LOG.md: docs/WORK_LOG.md
- AGENT_PROMPTS.md: docs/AGENT_PROMPTS.md
- AGENTS.md: AGENTS.md

## My request:
You are working on the CampusSignal repository.

IMPORTANT CORRECTION:
The current repository contains a Python/FastAPI prototype, but the submitted CS5013 proposal requires the project backend to use Java, Maven, and Spring Boot.

Your task is to begin the migration to the correct Java stack and implement the 11 September milestone in Java.

Do NOT delete the existing Python implementation yet. Treat it only as a behavioral reference until the equivalent Java functionality is working and tested.

\==================================================
SOURCE OF TRUTH
\==================================================

The submitted proposal requires:

Language/build:
\- Java
\- Maven

Backend:
\- Spring Boot

Email access:
\- Gmail API
\- Google OAuth 2.0

Database:
\- relational database

11 September 2026 milestone:
\- Gmail API integration with OAuth 2.0
\- retrieve emails from an authorized account
\- store:
&#x20; \- message ID
&#x20; \- subject
&#x20; \- sender
&#x20; \- timestamp
&#x20; \- body
\- basic duplicate handling
\- user profiles containing:
&#x20; \- programme
&#x20; \- year
&#x20; \- selected interests
\- initial category-score model so different users can have different preference scores for the same categories

Do NOT implement the October AI/NLP milestone in this task.

\==================================================
BEFORE CHANGING CODE
\==================================================

1\. Inspect the full repository.
2\. Inspect:
&#x20;  \- current Python Gmail implementation
&#x20;  \- current database models
&#x20;  \- current tests
&#x20;  \- AGENTS.md
&#x20;  \- docs/AGENT\_PROMPTS.md
&#x20;  \- docs/WORK\_LOG.md
3\. Run the current existing test suite and record the baseline result.
4\. Check the current Git branch.
5\. Do not work directly on main.

Create/switch to:

feature/java-springboot-migration

Do not delete, rewrite, or modify the working Python implementation unless absolutely necessary.

\==================================================
JAVA PROJECT FOUNDATION
\==================================================

Create a Maven + Spring Boot project inside the repository.

Use a clear Java package namespace based on CampusSignal.

Before choosing the Java version:
\- inspect the repository/course configuration for any required Java version
\- if none exists, use a current LTS Java version compatible with the selected Spring Boot release
\- document the chosen version and reason

Required dependencies should include only what is currently necessary, such as:

\- Spring Web
\- Spring Data JPA
\- Bean Validation
\- Gmail API client dependencies
\- Google OAuth client dependencies
\- relational database driver suitable for local development/testing
\- Spring Boot Test / JUnit

Do not add AI/LLM dependencies.

Suggested Java structure:

src/main/java/.../campussignal/
&#x20;   CampusSignalApplication.java

&#x20;   config/
&#x20;   controller/
&#x20;   dto/
&#x20;   entity/
&#x20;   repository/
&#x20;   service/
&#x20;   gmail/

src/main/resources/
&#x20;   application.properties
&#x20;   or
&#x20;   application.yml

src/test/java/.../campussignal/

Use normal Spring Boot conventions.

\==================================================
PORT EXISTING EMAIL FUNCTIONALITY
\==================================================

Use the current Python implementation only to understand the required behavior.

Implement Java equivalents for:

1\. Gmail OAuth authorization support
2\. Gmail API client/service
3\. Email retrieval
4\. Email persistence
5\. Duplicate protection

Create an Email entity with at least:

\- id
\- gmailMessageId
\- subject
\- sender
\- receivedAt / timestamp
\- body
\- createdAt

Requirements:

\- gmailMessageId must be unique
\- duplicate Gmail messages must not create duplicate database records
\- database access must use Spring Data JPA
\- Gmail-specific code must remain separated from core persistence/business logic
\- do not hard-code OAuth secrets
\- configuration must use environment variables/properties
\- do not commit credentials, client secrets, tokens, or private keys

If the current Python prototype contains Gmail watch/PubSub/history functionality, do NOT port those components unless they are required for the 11 September milestone.

The required milestone is retrieval, not real-time notification infrastructure.

\==================================================
USER PROFILE
\==================================================

Port the User Profile work into Java.

Create a UserProfile entity containing:

\- id
\- email
\- department
\- programme
\- year
\- semester (optional)
\- interests
\- createdAt

Requirements:

\- email unique and required
\- department required
\- programme required
\- year >= 1
\- semester optional, but if supplied >= 1
\- at least one interest required
\- normalize interests:
&#x20; \- trim whitespace
&#x20; \- lowercase
&#x20; \- remove duplicates while preserving order
\- interests must remain extensible
\- do NOT use a restrictive fixed enum for user interests yet

Use appropriate JPA mapping for interests.

Create request/response DTOs rather than exposing JPA entities directly through controllers.

\==================================================
USER PROFILE API
\==================================================

Implement at minimum:

POST /api/v1/profiles

Creates a user profile.

GET /api/v1/profiles/{id}

Retrieves a profile.

GET /api/v1/profiles/by-email/{email}

Retrieves a profile by email if this is cleanly supported by the architecture.

Use Bean Validation and return appropriate HTTP status codes.

Do not implement authentication/authorization for these profile endpoints unless the current application architecture already provides it.

\==================================================
INITIAL CATEGORY SCORE MODEL
\==================================================

Implement the initial category preference score required by the 11 September milestone.

The proposal defines:

CategoryScore(u, c)
\= 0.40E + 0.15P + 0.35B + 0.10R

where:

E = explicit preference
P = profile relevance
B = behavioural preference
R = related-interest similarity

All inputs must be normalized to [0, 1].

Implementation requirements:

\- create a dedicated CategoryScoreService
\- keep weights configurable
\- do not scatter numeric weights through business logic
\- do not implement final email relevance ranking
\- do not implement semantic embeddings
\- do not implement AI classification

Because this is the cold-start milestone and behavioral data may not yet exist:

\- support B using a neutral/default initial value
\- support R using a simple deterministic initial value or clearly documented placeholder behavior
\- explicit user-selected interests must meaningfully affect E
\- profile information must meaningfully affect P where applicable

Do not pretend that behavioral learning or semantic matching already exists.

The service should be testable independently.

Create a simple response object such as:

{
&#x20; "category": "internships",
&#x20; "score": 0.72,
&#x20; "components": {
&#x20;   "explicitPreference": 1.0,
&#x20;   "profileRelevance": 0.4,
&#x20;   "behavioralPreference": 0.5,
&#x20;   "relatedInterestSimilarity": 0.3
&#x20; }
}

Exact DTO naming may follow project conventions.

\==================================================
CATEGORY SCORE DEMONSTRATION
\==================================================

Add tests showing that two different users can receive different scores for the same category.

Example:

User A interests:
\- internships
\- research
\- hackathons

User B interests:
\- sports
\- cultural events
\- clubs

For category "internships":

score(User A, internships)
must be greater than
score(User B, internships)

This behavior is required for the September milestone.

\==================================================
TESTING
\==================================================

Add Java tests covering at least:

EMAIL:
\- email entity persistence
\- duplicate gmailMessageId protection
\- email retrieval service behavior where practical

PROFILE:
\- valid profile creation
\- invalid empty programme
\- invalid empty department
\- invalid year
\- invalid semester
\- empty interests rejected
\- interest trimming
\- interest lowercase normalization
\- duplicate interest removal
\- repository persistence
\- POST profile endpoint
\- GET profile endpoint

CATEGORY SCORE:
\- score stays in [0,1]
\- explicit selected interest increases the score
\- two users can have different scores for the same category
\- configured weights are used
\- missing behavioral history is handled explicitly and safely

Run the complete Maven test suite.

Do not finish with failing Java tests.

\==================================================
PYTHON CODE
\==================================================

Do NOT delete the Python code during this task.

Create a clear note in documentation that:

\- Python is the earlier prototype/reference implementation
\- Java/Spring Boot is the course-compliant implementation going forward
\- no new product features should be added to Python unless explicitly approved

Do not attempt a line-by-line translation.

Port behavior, not Python architecture.

\==================================================
DOCUMENTATION / TRACEABILITY
\==================================================

Existing documentation rules must continue to be followed.

Update:

docs/AGENT\_PROMPTS.md
docs/WORK\_LOG.md

AGENT\_PROMPTS.md:
Append this entire prompt verbatim.

Do not overwrite previous prompts.

WORK\_LOG.md:
Append a new entry:

\## Work Entry
Date: \<actual date>
Contributor: Priyanshi Garg
Milestone: 11 September 2026 — Email Retrieval and User Preference Setup
Stage: Java/Spring Boot Migration
Branch: \<actual branch>

\### Status
Completed / In Progress / Blocked

\### Reason for migration
Repository prototype was implemented in Python/FastAPI, while the submitted project proposal requires Java, Maven, and Spring Boot.

\### Work completed
\- actual completed work only

\### Python status
\- describe what remains as reference/prototype

\### Java implementation
\- actual modules implemented

\### Files changed
\- actual files only

\### Tests
\- Python baseline result
\- Java focused test results
\- final Maven test result

\### Git
Commit: \<actual hash or Pending>
PR: \<actual PR or Pending>
PR created by: \<actual person or Pending>

\### Next stage
\<actual next stage based on what remains>

Never invent:
\- commit hashes
\- PR numbers
\- PR URLs
\- authors
\- test results

\==================================================
AGENTS.MD
\==================================================

Update AGENTS.md with a short rule stating:

\- The course-compliant backend is Java + Maven + Spring Boot.
\- The existing Python implementation is a prototype/reference only.
\- New backend product functionality must be implemented in Java unless an explicit project decision changes the stack.
\- Every agent must continue maintaining AGENT\_PROMPTS.md and WORK\_LOG.md.

Do not remove existing traceability rules.

\==================================================
DO NOT IMPLEMENT
\==================================================

Do NOT implement:

\- AI email classification
\- LLM integration
\- email summarization
\- deadline extraction
\- eligibility extraction
\- event date extraction
\- embeddings
\- semantic similarity
\- final user-email relevance ranking
\- behavior-learning feedback
\- personalized feed
\- frontend
\- critical-message override

Those belong to later milestones.

\==================================================
SAFETY / SCOPE
\==================================================

Do not:
\- expose credentials
\- commit Google OAuth credentials
\- commit Gmail tokens
\- delete working Python code
\- refactor unrelated files
\- rewrite Git history
\- push directly to main
\- fabricate test results
\- fabricate PR information

If the existing repository architecture creates a serious conflict with this migration, stop and explain the conflict before making a destructive decision.

\==================================================
AT THE END
\==================================================

Report exactly:

1\. branch used
2\. Java version selected and why
3\. Spring Boot version
4\. Maven project structure created
5\. files created
6\. files modified
7\. Gmail functionality ported
8\. email persistence implemented
9\. duplicate handling implemented
10\. user profile functionality implemented
11\. category-score functionality implemented
12\. Python code intentionally left in place
13\. Python baseline test result
14\. Maven/JUnit test result
15\. git diff --stat
16\. git status
17\. whether a commit was created
18\. whether a PR was created
19\. anything still missing for the 11 September milestone

Do not claim the migration is complete unless every required September milestone component listed above is implemented and tested.

---

## Prompt Entry
Date: 2026-10-06 13:14:11 +05:30
Contributor: Codex
Branch: phase4-live-demo
Milestone: Email Classification Discovery and Validation
Task: Mailbox Evidence Analysis and Classifier Specification

### Prompt summary
Asked Codex to analyse approximately 70 recent IIT Madras Smail messages in read-only mode, split them into discovery and validation samples, infer and freeze a compact deterministic classifier taxonomy from discovery evidence, validate it, identify ambiguity and false-positive/false-negative risks, propose anonymised tests, and document findings without changing implementation or exposing private mailbox data. Required outputs include EMAIL_CLASSIFICATION_DISCOVERY.md, TEST_LOG.md, BUG_LOG.md, an appended work log, and a human-approval gate before Java implementation.

---

## Prompt Entry
Date: 2026-10-06 13:34:49 +05:30
Contributor: Codex
Branch: phase4-live-demo
Milestone: Deterministic Email Classification
Task: Classification Specification V1 Approval

### Prompt summary
Asked Codex to convert the mailbox discovery findings and human-approved category, scoring, confidence, precedence, filtering, normalisation, negation, quote, attachment, and fallback decisions into Classification Specification V1; mark the corresponding classifier tests as planned; update traceability; verify the existing Java suite; and make no production or test-code changes.

---

## Prompt Entry
Date: 2026-10-06 13:44:04 +05:30
Contributor: Codex
Branch: phase4-live-demo
Milestone: Deterministic Email Classification
Task: Classifier Test Batch 1 — RED Phase

### Prompt summary
Asked Codex to inspect the approved V1 specification and Java conventions, preserve the 77-test baseline, create exactly 20 deterministic synthetic JUnit classifier tests before business logic, add only the minimal compile contract needed to execute them in an intentional RED state, keep unrelated tests green, and update test/work traceability without changing Gmail, persistence, ranking, or other unrelated behavior.

---

## Prompt Entry
Date: 2026-10-06 15:50:36 +05:30
Contributor: Codex
Branch: phase4-live-demo
Milestone: Deterministic Email Classification
Task: Classifier Test Batch 1 — GREEN Phase

### Prompt summary
Asked Codex to implement the smallest V1-compliant deterministic rule-based email classifier needed to move the existing 20 Batch-1 JUnit tests from RED to GREEN without changing the tests or integrating Gmail, persistence, profile scoring, or ranking; then run focused and full regression suites and update project traceability.

---

## Prompt Entry
Date: 2026-10-06 16:06:05 +05:30
Contributor: Codex
Branch: phase4-live-demo
Milestone: Deterministic Email Classification
Task: Classifier Robustness Test Batch 2

### Prompt summary
Asked Codex to preserve the 97-test GREEN baseline, add exactly 20 synthetic JUnit robustness tests for V1 normalisation, filtering, negation, recognition, ties, repetition, HTML, facility surveys, academic registration, and wellbeing/event boundaries, run them against the unchanged Batch-1 classifier, record actual PASS/FAIL results, and stop before production-rule changes.

---

## Prompt Entry
Date: 2026-10-06 16:15:54 +05:30
Contributor: Codex
Branch: phase4-live-demo
Milestone: Deterministic Email Classification
Task: Classifier Test Batch 2 — GREEN Phase

### Prompt summary
Asked Codex to reproduce the six Batch-2 classifier failures, diagnose shared root causes, make the smallest V1-compliant production changes for facility feedback, nearby negation/confidence, unresolved ties, and the wellbeing/event boundary, preserve all test expectations, run focused and full verification, and update traceability without touching unrelated modules.

---

## Prompt Entry
Date: 2026-10-06 16:39:02 +05:30
Contributor: Codex
Branch: phase4-live-demo
Milestone: CS5013 Static Analysis
Task: Initial SpotBugs Analysis and Triage

### Prompt summary
Asked Codex to preserve the 117-test baseline, add only the Maven SpotBugs plugin, run an unsuppressed initial analysis, inspect and triage every finding rather than blindly fixing it, document rule meaning, risk, proposed action, and test implications, rerun the Java suite, and stop before production fixes.

---

## Prompt Entry
Date: 2026-10-06
Contributor: Codex
Branch: phase4-live-demo
Milestone: CS5013 Static Analysis
Task: Test-First SpotBugs Remediation

### Prompt summary
Asked Codex to redo the approved test-first remediation of the six-finding SpotBugs baseline: characterize and explicitly handle null history IDs, narrow the Pub/Sub handler's checked exception contract, preserve the Gmail command privacy boundary while making unexpected runtime failures safely diagnosable, leave the two false positives unchanged and unsuppressed, run focused then complete verification, compare SpotBugs before and after, update all required logs, and avoid unrelated changes, commits, or pushes. The exact detailed task prompt is preserved in this file under the matching task entry.

---

## Prompt Entry
Date: 2026-10-06
Contributor: Codex
Branch: phase4-live-demo
Milestone: CS5013 Static Analysis
Task: Initial PMD Maintainability Analysis and Triage

### Exact task prompt

> We have completed the SpotBugs phase of CampusSignal.
>
> Verified current state:
>
> - Branch: `phase4-live-demo`
> - Complete Java suite: 122 tests, 0 failures, 0 errors, 0 skipped
> - SpotBugs initial findings: 6
> - SpotBugs after remediation: 2
> - Remaining two findings are reviewed false positives and remain unsuppressed
> - No broad SpotBugs exclusions/suppressions were introduced
> - Genuine operational defect `CS-STA-001` was discovered and fixed during the SpotBugs remediation phase
>
> We are now beginning the CS5013 PMD / maintainability-analysis phase.
>
> This task is ANALYSIS FIRST.
>
> Do NOT automatically refactor or fix all PMD findings.
>
> ## 1. Preserve current baseline
>
> Before changing anything:
>
> - record branch
> - record current commit SHA
> - confirm working-tree state
> - run the complete Java suite
>
> Record the actual result.
>
> Expected baseline is currently:
>
> 122 tests
> 0 failures
> 0 errors
> 0 skipped
> BUILD SUCCESS
>
> Use the actual result rather than assuming this value.
>
> ## 2. Inspect Maven configuration
>
> Read `pom.xml`.
>
> Add a suitable Maven PMD plugin configuration compatible with the current Java/Spring Boot project.
>
> Use a reproducible Maven-based setup.
>
> Do not manually download standalone PMD binaries.
>
> Do not remove or alter the existing SpotBugs configuration.
>
> Do not add JaCoCo yet.
>
> ## 3. Initial PMD analysis
>
> Run PMD across the Java production source.
>
> Use a reasonable standard Java ruleset appropriate for maintainability/design analysis.
>
> Record:
>
> - PMD/plugin version
> - Maven command
> - rulesets used
> - total findings
> - affected class
> - method/location
> - rule name
> - priority/severity where available
>
> Do not suppress findings initially.
>
> ## 4. Pay particular attention to complexity
>
> Inspect PMD findings related to:
>
> - cyclomatic complexity
> - cognitive/NPath-style complexity if available
> - excessive method length
> - excessive class length
> - deep nesting
> - complicated conditional logic
> - duplicated conditional branches
> - unnecessary local variables
> - unused/private code
> - empty blocks/catches
> - overly broad design patterns
> - other maintainability/code-smell findings
>
> Pay particular attention to:
>
> `RuleBasedEmailClassificationService`
>
> because its behaviour is now protected by 40 classifier tests and it has accumulated multiple rule paths.
>
> However, do NOT assume it needs refactoring merely because it is large. Let PMD evidence and source inspection guide the decision.
>
> ## 5. Triage every meaningful finding
>
> For each PMD finding classify it as:
>
> - REAL MAINTAINABILITY ISSUE
> - MINOR STYLE ISSUE
> - FALSE POSITIVE / NOT APPLICABLE
> - NEEDS FURTHER INVESTIGATION
>
> For each finding record:
>
> 1. PMD rule.
> 2. What the rule means.
> 3. Exact CampusSignal code affected.
> 4. Why PMD flagged it.
> 5. Whether it actually harms readability/maintainability.
> 6. Whether changing it risks behaviour.
> 7. Smallest sensible refactoring, if justified.
> 8. Tests currently protecting the behaviour.
> 9. Whether additional characterization/regression tests would be needed before refactoring.
>
> Do not modify production code during this initial triage.
>
> ## 6. Complexity versus time complexity
>
> Do not confuse PMD complexity findings with algorithmic runtime complexity.
>
> If PMD reports high cyclomatic complexity, describe it as control-flow/maintainability complexity.
>
> Do not claim that reducing cyclomatic complexity automatically improves Big-O runtime.
>
> If any genuinely performance-sensitive algorithm is noticed, record it separately for later profiling rather than changing it in this task.
>
> ## 7. Documentation
>
> Create:
>
> `docs/PMD_ANALYSIS_LOG.md`
>
> Record:
>
> - date
> - branch
> - commit
> - PMD version
> - Maven configuration
> - command
> - baseline test result
> - initial findings
> - finding-by-finding triage
> - proposed remediation
> - refactoring risk
> - existing test protection
> - unresolved questions
>
> Append a concise task summary to:
>
> `docs/AGENT_PROMPTS.md`
>
> Append an execution entry to:
>
> `docs/WORK_LOG.md`
>
> Do not update `BUG_LOG.md` merely because PMD reports maintainability issues.
>
> Only genuine behavioural defects discovered during investigation belong in `BUG_LOG.md`.
>
> ## 8. Regression verification
>
> After adding PMD tooling/documentation only, rerun the complete Java test suite.
>
> The existing 122 tests should remain green.
>
> Record the actual result.
>
> ## 9. Restrictions
>
> Do NOT:
>
> - refactor production code yet
> - modify classifier behaviour
> - modify Gmail behaviour
> - modify database schema
> - modify profile scoring
> - implement ranking
> - add JaCoCo
> - add JFR/JMC
> - delete Python
> - add blanket PMD suppressions
> - change tests merely to satisfy PMD
> - commit or push automatically
>
> This task ends after initial PMD analysis and triage.
>
> ## 10. Completion report
>
> Report:
>
> 1. Branch and commit.
> 2. Baseline Java-suite result.
> 3. PMD/plugin version.
> 4. PMD ruleset used.
> 5. Command used.
> 6. Number of initial findings.
> 7. Findings by major category.
> 8. Most important maintainability/complexity findings.
> 9. False positives/not-applicable findings.
> 10. Classes/methods most worth refactoring.
> 11. Existing tests protecting those areas.
> 12. Files changed.
> 13. Post-tooling Java-suite result.
> 14. Confirmation that production behaviour was untouched.
> 15. Exact recommended next remediation step.
>
> Stop after PMD analysis and triage.

### Prompt summary

Asked Codex to preserve and verify the 122-test baseline; add reproducible Maven PMD tooling without changing SpotBugs or adding JaCoCo; run an unsuppressed production-Java analysis with explicit maintainability and complexity coverage; inspect and classify every meaningful finding, especially those in `RuleBasedEmailClassificationService`; distinguish control-flow complexity from runtime complexity; make no production/test behavior changes; document the full baseline, configuration, findings, risks, test protection, and next remediation step; rerun all tests; and stop before refactoring, committing, or pushing.

---

## 2026-10-06 — PMD remediation completion summary

The exact remediation prompt is recorded above under “PMD test-first maintainability remediation.” The task preserved the clean 122-test/32-finding baseline, added 10 focused characterization tests, sequentially refactored Gmail pagination, notification validation, history traversal, and classifier text preprocessing, ran focused/full/PMD verification after every group, left MIME traversal and stale-history cause preservation unchanged after investigation, finished with 132 green tests and 27 unsuppressed PMD findings, found no behavioral bug, and made no commit or push.

---

## 2026-10-06 — JaCoCo baseline task summary

The exact task prompt is recorded above under “CS5013 JaCoCo baseline analysis and triage.” The task required a clean 132-test baseline, JaCoCo HTML/XML/CSV reporting in the normal Maven test lifecycle without a coverage gate, package/class/branch analysis with risk-based gap triage, no production behavior changes or percentage-driven tests, full documentation, and a small prioritized next test batch.

---

## 2026-10-06 — Gmail history synchronization coverage batch

### Exact task prompt

> We have completed the initial JaCoCo baseline analysis for CampusSignal.
>
> Current verified baseline:
>
> - Branch: `phase4-live-demo`
> - Java tests: 132
> - Failures: 0
> - Errors: 0
> - Skipped: 0
> - Instruction coverage: 93.60%
> - Line coverage: 92.04%
> - Branch coverage: 82.77%
> - Method coverage: 92.51%
> - Class coverage: 100%
>
> JaCoCo is being used to identify meaningful missing behavior, NOT to maximize a percentage.
>
> This task is a very small TEST-ONLY coverage improvement batch.
>
> Do not modify production code unless a newly added test exposes a genuine behavioral defect.
>
> ## Scope
>
> Add exactly the three prioritized Gmail history synchronization tests identified in `docs/COVERAGE_LOG.md`.
>
> ### Test 1 — repeated pagination token
>
> Create a test proving that when Gmail history pagination returns a page token that repeats instead of making progress:
>
> - synchronization fails safely;
> - the repeated-page situation does not lead to an infinite pagination loop;
> - persisted Gmail history progress is NOT advanced;
> - previously stored progress remains unchanged.
>
> Use the existing Gmail/history test infrastructure and mocks.
>
> Do not change production behavior merely to make the test pass unless the current behavior is genuinely incorrect.
>
> ### Test 2 — partial stale-history recovery failure
>
> Create a test covering stale-history recovery where recovery begins but fails before completion.
>
> Verify that:
>
> - the failure propagates according to the existing contract;
> - persisted synchronization progress is NOT advanced;
> - the current Gmail profile/history ID is not read or used as a false success marker after incomplete recovery;
> - existing prior progress remains intact.
>
> This test should specifically protect against partially successful recovery being mistaken for completed synchronization.
>
> ### Test 3 — duplicate message with missing aggregate page history ID
>
> Create a test for a history page containing an already-existing/duplicate message where the aggregate page history ID is missing.
>
> Verify that:
>
> - the duplicate event is accounted for according to existing synchronization semantics;
> - no duplicate email is persisted;
> - synchronization progress still advances using the notification history ID when that is the existing documented fallback;
> - the final stored progress matches the expected fallback value.
>
> Do not invent new fallback semantics. Confirm the current intended contract from the source and existing tests.
>
> ## Test-first discipline
>
> For each scenario:
>
> 1. inspect current production implementation and related tests;
> 2. write the focused test;
> 3. run the focused `GmailRealtimeServiceTest`;
> 4. observe whether it passes or reveals a real defect;
> 5. only change production code if an actual behavioral defect is demonstrated.
>
> Do not weaken existing tests.
>
> Do not modify unrelated tests.
>
> ## Verification
>
> After all three tests:
>
> Run the focused history synchronization tests.
>
> Then run:
>
> `.\mvnw.cmd -B -ntp clean test`
>
> Record:
>
> - total test count;
> - failures;
> - errors;
> - skipped;
> - build result.
>
> Then inspect the JaCoCo XML/report.
>
> Compare before vs after:
>
> - overall branch coverage;
> - `GmailHistorySyncService` line coverage;
> - `GmailHistorySyncService` branch coverage;
> - newly covered branches.
>
> Do not introduce a coverage threshold.
>
> ## Documentation
>
> Update:
>
> `docs/COVERAGE_LOG.md`
>
> with:
>
> - the three scenarios;
> - before coverage;
> - tests added;
> - whether each passed immediately or exposed a defect;
> - after coverage;
> - newly covered branches;
> - interpretation.
>
> Update:
>
> `docs/TEST_LOG.md`
>
> with the new tests and their behavioral purpose.
>
> Append a concise summary to:
>
> `docs/AGENT_PROMPTS.md`
>
> Append execution details to:
>
> `docs/WORK_LOG.md`
>
> Update `docs/BUG_LOG.md` only if a test demonstrates a genuine production defect.
>
> ## Restrictions
>
> Do NOT:
>
> - add arbitrary tests for percentage improvement;
> - modify classifier behavior;
> - modify profile/category scoring;
> - modify controllers;
> - change database schema;
> - implement ranking;
> - refactor production code unnecessarily;
> - alter PMD or SpotBugs configuration;
> - add a JaCoCo coverage gate;
> - test trivial getters solely for coverage;
> - test JVM/bootstrap/framework code merely for coverage;
> - commit or push automatically.
>
> ## Completion report
>
> Report:
>
> 1. Starting test count.
> 2. Tests added.
> 3. Whether each test passed immediately.
> 4. Any genuine defects discovered.
> 5. Production files changed, if any.
> 6. Focused Gmail history test result.
> 7. Full Java-suite result.
> 8. Overall JaCoCo line/branch coverage before vs after.
> 9. GmailHistorySyncService coverage before vs after.
> 10. Important branches newly covered.
> 11. Files changed.
> 12. Exact recommended next step.
>
> Stop after these three Gmail history coverage tests.

### Prompt summary

Add exactly three behavior-focused Gmail history synchronization integration tests for repeated page-token failure, partial stale-history recovery failure, and duplicate handling with missing page history ID fallback; run the focused class after each addition; change production only for a demonstrated defect; rerun the 135-test suite with JaCoCo; document before/after coverage and protected branches; and stop without a coverage gate, unrelated changes, commit, or push.

---

## 2026-10-07 — Classifier ingestion/persistence integration

### Exact task prompt

> CampusSignal has completed the testing/static-analysis/coverage phase for the V1 classifier.
>
> Current verified state:
>
> - Branch: `phase4-live-demo`
> - Full Java suite: 137 tests
> - 0 failures
> - 0 errors
> - 0 skipped
> - Classifier tests: 45/45
> - Overall branch coverage: 84.35%
> - No further coverage-driven work is planned at this stage
>
> We are now beginning the first end-to-end product integration step:
>
> Integrate the existing `EmailClassificationService` into the Java email ingestion and persistence flow.
>
> This must be TEST-FIRST and must preserve existing ingestion semantics.
>
> ## 1. Inspect before changing
>
> First inspect:
>
> - `EmailIngestionService`
> - `EmailSource`
> - normalized/incoming email DTO/model used by ingestion
> - persisted Email entity
> - Email repository
> - duplicate/idempotency logic
> - current persistence tests
> - existing Gmail retrieval/history flow
> - `EmailClassificationService`
> - `RuleBasedEmailClassificationService`
> - classification result DTO/record
> - existing schema/data initialization
>
> Do not assume field names or architecture.
>
> Document the actual current flow from source input to persisted email.
>
> ## 2. Integration goal
>
> The desired architecture is conceptually:
>
> EmailSource
> → normalized email representation
> → EmailIngestionService
> → EmailClassificationService
> → persisted Email
>
> Classification must remain source-independent.
>
> Do NOT inject Gmail API objects directly into the classifier.
>
> The classifier should receive the same normalized subject/body representation regardless of whether the email originated from:
>
> - Gmail retrieval
> - Gmail history synchronization
> - another future EmailSource
>
> ## 3. Persistence goal
>
> Persist the V1 classification result with each newly ingested email.
>
> The persisted representation must include:
>
> - primary category
> - classification confidence
>
> Use the existing enum/result model where appropriate.
>
> Do not duplicate category definitions as strings if the existing enum can be used safely.
>
> Inspect current JPA/entity conventions before choosing enum persistence strategy.
>
> ## 4. Preserve duplicate protection
>
> This is critical.
>
> Existing duplicate/idempotency behavior must remain unchanged.
>
> If an email already exists:
>
> - do not create another persisted email;
> - do not accidentally increment creation counts;
> - preserve the existing `alreadyExisted` semantics;
> - do not make classification introduce a second persistence path.
>
> Determine whether existing duplicates should be reclassified during ordinary ingestion.
>
> For this integration milestone, prefer:
>
> existing email
> → retain existing persisted record
> → do not silently reclassify it
>
> unless the existing architecture/spec explicitly says otherwise.
>
> Document the decision.
>
> ## 5. Write tests BEFORE production implementation
>
> Add focused integration/service tests proving at least these behaviors.
>
> ### A. New email is classified and persisted
>
> Given a new normalized email with clear academic/career/event evidence:
>
> - ingestion calls classification;
> - email is persisted once;
> - persisted category matches expected classifier result;
> - persisted confidence matches expected classifier confidence.
>
> Do not duplicate the classifier's 45 unit tests here.
>
> Use only a small number of representative integration cases.
>
> ### B. Duplicate email remains idempotent
>
> Given an email that is already persisted:
>
> - ingestion does not create a duplicate;
> - existing duplicate accounting remains correct;
> - classification integration does not cause an additional insert.
>
> If classification should not run for duplicates, verify that using a mock where appropriate.
>
> ### C. GENERAL_OTHER / LOW persists correctly
>
> Use a message with no specialist evidence.
>
> Verify that the persisted classification is:
>
> - `GENERAL_OTHER`
> - corresponding LOW confidence representation
>
> ### D. Classification failure behavior
>
> Inspect the current classifier contract.
>
> If classification can throw, determine the correct ingestion policy before implementing.
>
> Do not silently swallow unexpected classification failures.
>
> Prefer maintaining transactional consistency:
>
> - no partially persisted email with missing classification unless explicitly designed;
> - failure should remain diagnosable.
>
> Write a test if this is a real reachable path.
>
> ### E. Source independence
>
> At least one test should demonstrate that `EmailIngestionService` classifies the normalized incoming model rather than Gmail-specific API types.
>
> Do not add Gmail coupling to the classification service.
>
> ## 6. Schema / entity changes
>
> If the persisted Email entity needs new fields, make the smallest schema change necessary.
>
> Likely concepts:
>
> - category
> - classification confidence
>
> But inspect actual code before choosing names/types.
>
> Preserve compatibility with the current H2 setup and existing persistence tests.
>
> Do not introduce unrelated schema redesign.
>
> Do not implement relevance ranking yet.
>
> ## 7. Confidence representation
>
> The V1 classifier currently uses deterministic confidence levels:
>
> - HIGH
> - MEDIUM
> - LOW
>
> with existing persisted/API numeric representation where applicable:
>
> - HIGH → 0.9
> - MEDIUM → 0.7
> - LOW → 0.5
>
> Do not reinterpret these as calibrated probabilities.
>
> Reuse the existing model rather than creating a second confidence system.
>
> ## 8. Transactional behavior
>
> Inspect whether ingestion currently uses Spring transactions.
>
> Ensure classification and persistence have sensible atomic behavior.
>
> If classification succeeds but persistence fails, normal transaction semantics should apply.
>
> If classification fails before persistence, do not leave a partially classified record.
>
> Do not add transaction complexity unnecessarily if the existing service already handles it correctly.
>
> ## 9. Regression requirements
>
> After implementation run:
>
> 1. new focused classification-ingestion tests;
> 2. existing persistence tests;
> 3. Gmail retrieval/history tests;
> 4. all 45 classifier tests;
> 5. complete Java suite.
>
> The existing 137 tests must remain unchanged and passing unless the entity/schema evolution legitimately requires a test fixture update.
>
> Do not weaken existing assertions.
>
> ## 10. Static-analysis regression
>
> After the complete test suite passes:
>
> Run SpotBugs.
>
> Run PMD.
>
> Do not automatically remediate unrelated existing findings.
>
> Confirm that the integration did not introduce important new correctness or maintainability findings.
>
> JaCoCo may be rerun through the normal test build, but do not begin another percentage-chasing phase.
>
> ## 11. Documentation
>
> Update:
>
> `docs/TEST_LOG.md`
>
> Record the new integration tests and why they exist.
>
> Update:
>
> `docs/WORK_LOG.md`
>
> Record:
>
> - pre-integration architecture
> - files changed
> - entity/schema decision
> - duplicate handling decision
> - transactional behavior
> - focused/full test results
>
> Append a concise summary to:
>
> `docs/AGENT_PROMPTS.md`
>
> If a genuine new defect is discovered, update:
>
> `docs/BUG_LOG.md`
>
> Do not create a bug entry merely because integration required new fields.
>
> ## 12. Restrictions
>
> Do NOT:
>
> - change classifier rules
> - change classifier evidence lists
> - change category precedence
> - change confidence mapping
> - re-open PMD refactoring
> - chase JaCoCo percentages
> - implement personalized ranking yet
> - create frontend work
> - delete Python
> - change Gmail API behavior unnecessarily
> - bypass existing duplicate protection
> - couple classifier directly to Gmail API classes
> - commit or push automatically
>
> ## 13. Completion report
>
> Report:
>
> 1. Existing ingestion architecture discovered.
> 2. Tests added before production implementation.
> 3. Whether any tests initially failed and why.
> 4. Entity/schema changes.
> 5. Production files changed.
> 6. How classifier was injected/invoked.
> 7. Duplicate/idempotency behavior.
> 8. Classification failure behavior.
> 9. Focused test results.
> 10. Complete Java-suite result.
> 11. SpotBugs result.
> 12. PMD result.
> 13. Whether any genuine bugs were discovered.
> 14. Confirmation that Gmail behavior and classifier rules were preserved.
> 15. Exact next recommended step.
>
> Stop after classifier → ingestion → persistence integration.
>
> Do NOT implement personalized relevance ranking in this task.

### Prompt summary

Integrate the existing source-independent V1 classifier into normalized Java email ingestion and persistence using tests first; persist category and confidence only for newly created emails; preserve duplicate, transaction, Gmail, and classifier semantics; run focused, full, SpotBugs, and PMD verification; document the architecture and decisions; and stop before relevance ranking.

---
