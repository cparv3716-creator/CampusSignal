# CS5013 Code Coverage Baseline and Triage

## Run metadata

- Date: 2026-10-06 (Asia/Calcutta)
- Branch: `phase4-live-demo`
- Starting commit: `ac81e06b34a210ad8233a3093c7d8143d9c67c18`
- Starting worktree: clean (`git status --short` produced no output)
- Java runtime: `java version "26.0.2" 2026-07-21`, Java HotSpot 64-Bit Server VM `26.0.2+10-55`
- Compiler target: Java 25 (`<java.version>25</java.version>`)
- JaCoCo Maven Plugin: 0.8.15
- Baseline and coverage command: `.\mvnw.cmd -B -ntp clean test`
- Reports: `target/site/jacoco/index.html`, `jacoco.xml`, and `jacoco.csv`
- Coverage gates: none; this is an observation-only baseline

## Clean pre-tooling baseline

The complete suite passed before `pom.xml` was changed:

- Tests run: 132
- Failures: 0
- Errors: 0
- Skipped: 0
- Result: BUILD SUCCESS
- Maven total time: 38.247 seconds

No JaCoCo configuration existed in `pom.xml` before this task.

## Maven configuration

`pom.xml` now declares `jacoco.version` 0.8.15 and configures:

- `prepare-agent` to write the agent options to `jacocoArgLine`;
- Surefire late property evaluation (`@{jacocoArgLine}`) so the JaCoCo and existing Mockito agents are both retained;
- `report` in the `test` phase, so the normal `clean test` command produces HTML, XML, and CSV;
- no `check` execution, minimum threshold, class exclusion, or package exclusion.

SpotBugs and PMD configuration was not removed or changed.

## Post-tooling verification

The same complete command passed after JaCoCo was configured:

- Tests run: 132
- Failures: 0
- Errors: 0
- Skipped: 0
- Result: BUILD SUCCESS
- Maven total time: 50.454 seconds
- JaCoCo bundle: 45 classes

## Project coverage

| Metric | Covered | Missed | Total | Coverage |
|---|---:|---:|---:|---:|
| Instructions | 4,578 | 313 | 4,891 | 93.60% |
| Lines | 694 | 60 | 754 | 92.04% |
| Branches | 365 | 76 | 441 | 82.77% |
| Methods | 173 | 14 | 187 | 92.51% |
| Classes | 45 | 0 | 45 | 100.00% |

The 100% class figure does not mean every class is thoroughly tested. JaCoCo marks a class covered when any of its executable instructions run. For example, `CampusSignalApplication` is a covered class while its `main` method is completely uncovered.

## Package coverage

Repository interfaces contain no instrumentable implementation bytecode, so JaCoCo shows the repository package without counters.

| Package | Instructions | Lines | Branches | Methods | Classes |
|---|---:|---:|---:|---:|---:|
| `edu.campussignal` | 3/18 (16.7%) | 1/5 (20.0%) | 0/2 (0.0%) | 1/2 (50.0%) | 1/1 (100%) |
| `edu.campussignal.config` | 208/222 (93.7%) | 25/28 (89.3%) | 15/20 (75.0%) | 10/10 (100%) | 3/3 (100%) |
| `edu.campussignal.controller` | 56/56 (100%) | 13/13 (100%) | n/a | 9/9 (100%) | 2/2 (100%) |
| `edu.campussignal.dto` | 324/330 (98.2%) | 43/43 (100%) | 19/24 (79.2%) | 11/11 (100%) | 9/9 (100%) |
| `edu.campussignal.entity` | 176/185 (95.1%) | 56/59 (94.9%) | n/a | 30/33 (90.9%) | 3/3 (100%) |
| `edu.campussignal.gmail` | 953/1,079 (88.3%) | 209/233 (89.7%) | 115/149 (77.2%) | 39/42 (92.9%) | 9/9 (100%) |
| `edu.campussignal.pubsub` | 25/63 (39.7%) | 9/21 (42.9%) | n/a | 3/5 (60.0%) | 1/1 (100%) |
| `edu.campussignal.service` | 2,833/2,938 (96.4%) | 338/352 (96.0%) | 216/246 (87.8%) | 70/75 (93.3%) | 17/17 (100%) |

## Class coverage highlights

The lowest line-covered production classes are:

| Class | Lines | Branches | Methods | Interpretation |
|---|---:|---:|---:|---|
| `CampusSignalApplication` | 1/5 (20.0%) | 0/2 (0.0%) | 1/2 | Bootstrap `main` is not invoked by Spring tests. |
| `GmailListenerService` | 5/13 (38.5%) | n/a | 1/3 | Constructor runs, but listener orchestration and its handler do not. |
| `PubSubPullSubscriber` | 9/21 (42.9%) | n/a | 3/5 | Ack/nack dispatch is covered; the blocking Google subscriber lifecycle is not. |
| `GmailAuthorizationService` | 15/31 (48.4%) | 7/16 (43.8%) | 3/6 | Flow/credential logic is partly tested; real transport/browser/client assembly is not. |
| `GmailProperties` | 11/14 (78.6%) | 5/10 (50.0%) | 5/5 | Happy-path shorthand names run; missing and fully-qualified configuration paths do not. |
| `GmailWatchState` | 21/24 (87.5%) | n/a | 9/12 | Only three passive persistence getters are uncalled. |

Coverage for the requested risk-focused classes is:

| Class | Instructions | Lines | Branches | Methods |
|---|---:|---:|---:|---:|
| `RuleBasedEmailClassificationService` | 1,581/1,610 (98.2%) | 113/115 (98.3%) | 87/102 (85.3%) | 15/15 |
| `ClassificationTextPreprocessor` | 384/384 (100%) | 64/64 (100%) | 43/46 (93.5%) | 11/11 |
| `GmailApiClient` | 319/343 (93.0%) | 71/75 (94.7%) | 47/60 (78.3%) | 10/10 |
| `GmailNotificationDecoder` | 108/108 (100%) | 26/26 (100%) | 20/22 (90.9%) | 5/5 |
| `GmailHistorySyncService` | 265/281 (94.3%) | 62/66 (93.9%) | 28/34 (82.4%) | 11/12 |
| `GmailCommand` | 165/167 (98.8%) | 41/41 (100%) | 6/7 (85.7%) | 5/5 |
| `GmailMessageMapper` | 261/277 (94.2%) | 48/52 (92.3%) | 35/44 (79.5%) | 11/11 |
| `EmailIngestionService` | 81/83 (97.6%) | 15/15 (100%) | 2/2 (100%) | 4/5 |
| `EmailRetrievalService` | 61/61 (100%) | 16/16 (100%) | 8/8 (100%) | 2/2 |
| `CategoryScoreService` | 163/163 (100%) | 24/24 (100%) | 20/26 (76.9%) | 4/4 |
| `ProfileService` | 38/38 (100%) | 5/5 (100%) | n/a | 4/4 |

Classes with potentially misleading high line coverage but lower branch coverage include `CategoryScoreService` (100% lines, 76.9% branches), `GmailCommand` (100%, 85.7%), `GmailApiClient` (94.7%, 78.3%), `GmailMessageMapper` (92.3%, 79.5%), `GmailHistorySyncService` (93.9%, 82.4%), and `RuleBasedEmailClassificationService` (98.3%, 85.3%). DTO constructors also show 100% lines but only 66.7% branch coverage for `IncomingEmail` and 70.0% for `ProfileCreateRequest`; those missed outcomes are mostly input-validation combinations rather than core routing behavior.

## Completely uncovered compiled methods

No production class is completely uncovered. Fourteen compiled methods or lambdas have zero instructions executed:

- `CampusSignalApplication.main`
- `PubSubPullSubscriber.listen` and its receiver lambda
- `GmailListenerService.listen` and its handler lambda
- `GmailAuthorizationService.authorize()`, `client()`, and the request-initializer lambda in `client()`
- `GmailWatchState.getId`, `getCreatedAt`, and `getUpdatedAt`
- exception-supplier lambdas in `EmailIngestionService.ingest`, `GmailHistorySyncService.sync`, and `GmailWatchStateService.advance`

The lambdas in the last item correspond to unexecuted error outcomes, not wholly untested services.

## Coverage-gap triage

| Classification | Class/method and uncovered region | Scenario that executes it | CampusSignal relevance | Appropriate test and decision |
|---|---|---|---|---|
| **IMPORTANT BEHAVIORAL GAP** | `GmailHistorySyncService.sync`, lines 55-58 and 64-65 | A history event references an already-persisted message; a page omits its aggregate history ID and progress falls back to the notification ID. | Duplicate-safe redelivery and correct progress are central to reliable ingestion. | Spring integration test with the existing H2 repository and a mocked Gmail client. **Add in the first batch.** |
| **FAILURE-PATH GAP** | `GmailHistorySyncService.loadAllPages`, lines 83-86 | Gmail repeats a nonblank history page token, or returns a blank token. | A repeated token could otherwise loop forever; a blank token must stop cleanly. | Focused service/integration test asserting the exception and unchanged progress. **Add repeated-token coverage now; blank-token case may share the test batch.** |
| **FAILURE-PATH GAP** | `GmailHistorySyncService.recoverFromStaleHistory`, lines 93-94 | Bounded stale-history recovery reports one or more failed messages. | Advancing state after partial recovery would lose retryable mail. | Integration test asserting `IOException`, unchanged state, and no `currentHistoryId` call. **Add in the first batch.** |
| **FAILURE-PATH GAP** | `GmailHistorySyncService.sync`, lines 36-37 | A notification arrives before watch state exists. | The operator needs a clear failure and Pub/Sub nack instead of silent loss. | Service integration test for the established exception; useful but secondary to recovery/pagination. **Add soon.** |
| **FAILURE-PATH GAP** | `GmailApiClient.watchInbox`, lines 88-98, and `currentHistoryId`, lines 127-128 | Blank topic; Gmail returns missing/negative history ID or expiration; profile lacks a usable history ID. | Provider malformation/configuration must fail before bad state is stored. | Queued-HTTP adapter tests with exact safe exception assertions. **Add after the history batch.** |
| **IMPORTANT BEHAVIORAL GAP** | `GmailApiClient.listHistoryPage`, lines 113-114 | A second history page supplies a nonblank page token. | The service's multi-page test mocks this adapter, so it does not verify query serialization to Gmail. | Adapter test asserting `pageToken` in the captured URL. **Add after the history batch.** |
| **LOW-RISK INFRASTRUCTURE GAP** | `GmailApiClient` constructor, line 33 | Null or blank configured user ID defaults to `me`. | Useful configuration hardening, but the default constructor already uses `me`. | Small adapter test if configuration work is being done; **defer**. |
| **IMPORTANT BEHAVIORAL GAP** | `RuleBasedEmailClassificationService.eventEvidence`, lines 204-205 | Subject has `film`, `dance`, or `activity` plus a time/location cue but no `screening`, `exhibition`, `tutorial`, or `invitation`. | This is an explicit classifier rule whose true outcome is not exercised. | Behavior-level classifier test asserting category and confidence. **Add in the next classifier batch.** |
| **IMPORTANT BEHAVIORAL GAP** | `RuleBasedEmailClassificationService.classifyCurrent`, line 42 | A category cue is negated in the subject, causing the body scan to short-circuit. | Negation affects category evidence and confidence; existing negation tests place cues in the body. | Behavior-level subject-negation test with exact category/confidence. **Add in the next classifier batch.** |
| **IMPORTANT BEHAVIORAL GAP** | Classifier lines 94, 104, 139, 150, 187, 191, 193, and 208 | Unequal strong academic/event purposes; graduate-role context without an application cue; individual feedback, recognition, participation, and event compound-condition outcomes. | These are real decision branches, but existing precedence tests already protect the principal category order. | Add only specification-backed examples with category/confidence assertions. **Defer and select cases from real/spec examples; do not enumerate conditions mechanically.** |
| **LOW-RISK INFRASTRUCTURE GAP** | Classifier quote rescue, lines 33-34 | Current text and quoted text both remain `GENERAL_OTHER`. | Observable result is the same as the existing general fallback. | A semantic test is possible, but it adds little fault-detection value. **Defer.** |
| **INTENTIONALLY NOT UNIT-TESTED** | Classifier `result`, lines 229-232 | `result` receives qualifying evidence with a zero score. | Current `Evidence.qualifies()` guarantees a positive strong weight, so the LOW arm is unreachable through the production API. | Do not use reflection or fabricate impossible state merely for coverage. **Defer unless production invariants change.** |
| **LOW-RISK INFRASTRUCTURE GAP** | `ClassificationTextPreprocessor`, lines 29, 80, and 124 | Blank search phrase, the remaining negation token form (notably `non`), or direct null/blank normalization variants. | Production phrase tables are nonblank; nulls are sanitized at the public classifier boundary. Negation-token behavior can matter. | Cover `non` only through a meaningful classifier example; do not directly test private helpers for a percentage. **Defer to classifier batch.** |
| **FAILURE-PATH GAP** | `GmailCommand.run`, line 53 | A retrieve command completes with `failed == 0` and returns exit code 0. | CLI success status is externally observable. | Unit test with a successful `RetrievalSummary`, including printed counts. **Add soon; low cost.** |
| **IMPORTANT BEHAVIORAL GAP** | `GmailListenerService.listen`, lines 22-29 | Subscriber invokes the registered handler, which decodes and synchronizes the notification. | This is the seam connecting Pub/Sub delivery to history ingestion and is currently only constructed, not executed. | Mock subscriber, capture its handler, invoke it, and assert decoder/history arguments and summary output. **Add soon.** |
| **NEEDS INTEGRATION TEST RATHER THAN UNIT TEST** | `PubSubPullSubscriber.listen`, lines 23-37 | Build, start, block on, and stop a real Google subscriber. | Lifecycle, credentials, threading, and shutdown behavior depend on Google Pub/Sub infrastructure. | Emulator/controlled integration test or manual live-demo verification. `dispatch` ack/nack is already meaningfully unit tested. **Defer.** |
| **FAILURE-PATH GAP** | `EmailIngestionService.ingest`, line 39 exception supplier | Insert raises `DataIntegrityViolationException`, but no concurrent winner is found afterward. | The original database failure must propagate rather than be converted into a false duplicate. | Focused service test with controlled repository/transaction behavior, asserting the same failure propagates. **Add after Gmail history risks.** |
| **IMPORTANT BEHAVIORAL GAP** | `CategoryScoreService.profileRelevance`, lines 43-46 | Alternative postgraduate programme names and each engineering/BTech route independently select the high relevance value. | Category scoring is user-visible, but existing tests already cover high/low examples and normalization. | Parameterized exact-component tests for each documented route. **Defer to a scoring-specific batch.** |
| **FAILURE-PATH GAP** | `GmailMessageMapper`, lines 21-22, 40, 46-47, 54-57, 79-85, and 104 | Null Gmail message; null/malformed headers; malformed sender; disposition-only attachment; nested empty parts; invalid charset fallback. | Provider MIME variability is a realistic ingestion risk. | Focused mapper characterizations with exact mapped body/sender/timestamp or exception. **Add after the first history batch; these align with the prior PMD analysis.** |
| **FAILURE-PATH GAP** | `GmailNotificationDecoder`, lines 28 and 36 | Non-object/null parse alternatives and a non-textual `emailAddress`. | Invalid Pub/Sub payloads must nack safely. Existing tests already cover malformed JSON, blank JSON, array roots, missing/blank email, invalid/negative IDs, and exact messages. | Add one non-textual-email case if decoder work resumes. **Low priority; defer.** |
| **NEEDS INTEGRATION TEST RATHER THAN UNIT TEST** | `GmailAuthorizationService.authorize()` and `client()`, lines 56-73 | Local browser OAuth, loopback callback, trusted transport, credential application, and timeouts. | Critical for the live demo, but unit tests would mostly mock Google internals. Flow scope, offline access, stored refresh, missing credential file, and noninteractive failure are already asserted. | Controlled OAuth integration/manual smoke test with synthetic credentials where feasible. **Do not chase with superficial unit tests.** |
| **LOW-RISK INFRASTRUCTURE GAP** | `GmailProperties.qualifiedPubsubName`, lines 41-49 | Missing topic/subscription, fully-qualified name, or shorthand without project ID. | Configuration failures should be actionable but do not alter business routing. | Small configuration unit tests. **Defer behind data-loss/error-handling paths.** |
| **GENERATED/FRAMEWORK/BOOTSTRAP CODE** | `CampusSignalApplication.main`; repository interfaces; passive entity getters; record/framework plumbing | Launch JVM, trigger `System.exit`, or call generated/passive accessors. | Spring context loading already proves application wiring; direct `System.exit` tests are brittle and low value. | Application smoke/integration test where needed; no getter-only or `main`-only unit tests. **Intentionally leave uncovered.** |

## Existing test quality in context

JaCoCo measures which bytecode executed; it does **not** measure whether assertions are correct, whether the right behavior was specified, or whether a test would detect a realistic regression.

Strong existing examples include:

- Gmail pagination tests assert ordered IDs, request count, `INBOX`, `maxResults`, page-token serialization, blank-token stopping, duplicate filtering, provider propagation, and repeated-token rejection.
- Gmail notification tests assert accepted numeric/string IDs, malformed structures, negative/nonnumeric IDs, and established safe error messages.
- History integration tests assert database effects and progress invariants, including no advance after a fetch failure and bounded stale-history recovery.
- Persistence tests assert every stored field, the database uniqueness constraint, repeated ingestion, and a concurrent duplicate race.
- Classifier tests assert both category and confidence across precedence, quote isolation/rescue, negation, normalization, tie handling, facility feedback, and selective versus non-selective participation.
- Command and Pub/Sub tests assert that private provider details are not printed/logged and that messages are acknowledged only after successful processing.
- Profile and category-score tests assert HTTP validation/status behavior, normalized persistence, component values, weighting, cold-start defaults, and user-specific differences.

Repeated execution is also visible. Forty-three classifier tests repeatedly execute normalization and the same chain of negative category checks; many reach the same HIGH/MEDIUM confidence paths. The 26 profile API test invocations repeatedly traverse controller/exception-handler plumbing. This execution is useful but means the global percentage cannot be read as 132 independent business paths.

Examples where line coverage overstates assurance are `CategoryScoreService` (every line, six branches missed), `GmailCommand` (every line, successful retrieval exit branch missed), and `GmailApiClient` (94.7% lines while invalid watch/profile responses and history page-token request handling remain uncovered).

## Prioritized coverage work

No tests were added in this baseline task. The recommended order is:

1. **First batch — Gmail history safety (three focused integration tests):** repeated history page token throws without advancing state; stale-history recovery with any failed message throws without advancing or reading a new current history ID; an already-persisted message plus a page with no aggregate history ID counts `alreadyExisted` and advances to the notification ID.
2. **Second batch — adapter/orchestration failures:** invalid watch/profile responses, nonblank history `pageToken` serialization, successful retrieve command exit code, and captured `GmailListenerService` handler orchestration.
3. **Third batch — specification-backed classifier branches:** subject-side negation and the composite subject-only event rule, followed only by real examples for any remaining precedence/confidence branches.
4. **Then MIME and scoring:** the previously identified MIME attachment/fragment/charset cases and parameterized exact category-profile relevance routes.

The exact next step is to add only the three Gmail history safety tests in item 1, run the focused `GmailRealtimeServiceTest`, then run `.\mvnw.cmd -B -ntp clean test` and compare the new XML branch counters before selecting another batch. Do not add a coverage gate until the team has agreed which infrastructure and bootstrap code belongs in the measured target.

## Scope confirmation

This task changed Maven coverage tooling and documentation only. It did not change production Java behavior, classifier rules, Gmail behavior, database schema, ranking, Python code, PMD, or SpotBugs, and it added no percentage-driven tests. No commit or push was created.

---

## 2026-10-06 — Gmail history safety coverage batch

### Scope and starting point

This test-only batch implemented exactly the three history scenarios prioritized above. Starting state: 132 tests; 694/754 lines (92.04%); 365/441 branches (82.77%). `GmailHistorySyncService` started at 62/66 lines (93.94%) and 28/34 branches (82.35%).

### Tests added

| Test | Scenario and assertions | First focused result | Defect? |
|---|---|---|---|
| `repeatedHistoryPageTokenFailsWithoutAdvancingProgress` | Two pages return the same nonblank next token. Synchronization throws the established safe `IOException`, makes exactly two page calls, performs no fetch, and retains progress `100`. | PASS — focused class 8/8 | No |
| `partialStaleHistoryRecoveryFailureDoesNotAdvanceProgress` | Stale recovery stores one successfully fetched message but another fetch fails. The bounded-recovery failure propagates, prior progress remains `100`, and `currentHistoryId()` is never called. | PASS — focused class 9/9 | No |
| `duplicateMessageWithoutPageHistoryIdUsesNotificationProgress` | A page without an aggregate history ID references an already-persisted message. The summary reports one existing message and zero created, repository count remains one, and progress advances to notification ID `110`. | PASS — focused class 10/10 | No |

All three tests passed against the existing implementation on their first focused run. No production defect was demonstrated and no production file was changed.

### Verification

- Focused command after each addition: `.\mvnw.cmd -B -ntp "-Dtest=GmailRealtimeServiceTest" test`.
- Ordered focused results: 8/8, 9/9, and 10/10; all with 0 failures, 0 errors, and 0 skipped.
- Full command: `.\mvnw.cmd -B -ntp clean test`.
- Full result: BUILD SUCCESS — 135 tests, 0 failures, 0 errors, 0 skipped in 47.641 seconds.
- No JaCoCo threshold was introduced.

### Coverage comparison

| Metric | Before | After | Change |
|---|---:|---:|---:|
| Overall instructions | 4,578/4,891 (93.60%) | 4,589/4,891 (93.83%) | +11 instructions, +0.23 pp |
| Overall lines | 694/754 (92.04%) | 697/754 (92.44%) | +3 lines, +0.40 pp |
| Overall branches | 365/441 (82.77%) | 369/441 (83.67%) | +4 branches, +0.90 pp |
| Overall methods | 173/187 (92.51%) | 173/187 (92.51%) | unchanged |
| Overall classes | 45/45 (100%) | 45/45 (100%) | unchanged |
| `GmailHistorySyncService` lines | 62/66 (93.94%) | 65/66 (98.48%) | +3 lines, +4.54 pp |
| `GmailHistorySyncService` branches | 28/34 (82.35%) | 32/34 (94.12%) | +4 branches, +11.77 pp |

Percentages in the original baseline were rounded to two decimal places; the comparison uses exact JaCoCo counters.

### Newly covered behavior

- `sync`, lines 55-58: the `created == false` outcome now verifies duplicate accounting through `alreadyExisted`.
- `sync`, lines 64-65: the `page.getHistoryId() == null` outcome now verifies fallback to the notification history ID.
- `loadAllPages`, lines 83-84: the repeated-token outcome now verifies finite failure and unchanged progress.
- `recoverFromStaleHistory`, lines 93-94: the `recovery.failed() != 0` outcome now verifies failure propagation before profile history lookup or state advance.

The remaining two history branches are blank-token short-circuit/termination outcomes at lines 83 and 86; the only remaining uncovered history line is the missing-watch-state exception at line 37. They were deliberately left outside this exact three-test batch.

### Interpretation and next coverage target

The increase is useful because it represents four data-loss/error-handling decisions, not because it raises the global percentage. The existing production contract was confirmed: partial recovery may persist successful messages for idempotent retry, but progress advances only after complete recovery; page-level history ID is optional and the notification ID is the fallback.

The next bounded batch should cover the adapter/orchestration scenarios already ranked second: nonblank Gmail history page-token serialization, invalid watch/profile responses, successful retrieve command exit status, and the captured `GmailListenerService` handler. Keep that as a separate task and continue to avoid a coverage gate.
