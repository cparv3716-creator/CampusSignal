# PMD Analysis Log

## 2026-10-06 — Initial maintainability analysis and triage

Status: **INITIAL TRIAGE COMPLETE — NO PRODUCTION OR TEST CODE CHANGED**

### Baseline

- Branch: `phase4-live-demo`
- Commit: `4544610b5341f692a93006fe8bb156a7de65897f`
- Starting tracked worktree: clean
- Project source/target level: Java 25
- Analysis runtime: Java 26.0.2; no JDK 25 installation is available on this machine
- First command: `.\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -Ddebug=false test`
- First result: **BUILD FAILURE — 122 tests run, 0 failures, 45 errors, 0 skipped in 48.961 seconds**. Maven considered compilation up to date while required files such as `JsonConfiguration.class` and `RuleBasedEmailClassificationService.class` were absent from `target/classes`.
- Clean verification command: `.\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -Ddebug=false clean test`
- Authoritative clean baseline: **BUILD SUCCESS — 122 tests run, 0 failures, 0 errors, 0 skipped in 1 minute 18 seconds**.
- Baseline interpretation: the first failure was stale/incomplete generated build output, not a source or product defect. A clean compile rebuilt 41 production source files and restored the verified suite without changing tracked source.

### Maven PMD configuration

- Maven PMD plugin: `org.apache.maven.plugins:maven-pmd-plugin:3.28.0`
- PMD runtime: `7.27.0`
- Why the runtime is overridden: plugin 3.28.0 bundles PMD 7.17.0, which supports Java 25 source but could not read JDK 26 class-file major version 70. PMD 7.27.0 supports the executing JDK and Java 25 source analysis.
- PMD runtime modules are version-locked through the Maven plugin dependencies (`pmd-core`, `pmd-java`, `pmd-javascript`, and `pmd-jsp`) following the plugin's supported runtime-upgrade mechanism.
- Target JDK/source language: `${java.version}` = `25`.
- Scope: production Java only (`includeTests=false`).
- Minimum priority: 5, meaning priorities 1 through 5 are eligible.
- Cross-reference generation: disabled (`linkXRef=false`).
- Suppressions/exclusions: none.
- SpotBugs configuration: preserved unchanged.
- JaCoCo, JFR, and JMC: not added.

### Ruleset

Checked-in ruleset: `config/pmd/ruleset.xml`

The ruleset starts with PMD's standard `rulesets/java/quickstart.xml` and adds these focused design rules, which quickstart intentionally leaves disabled:

- `AvoidDeeplyNestedIfStmts`
- `CognitiveComplexity`
- `CollapsibleIfStatements`
- `CyclomaticComplexity`
- `ExcessiveParameterList`
- `GodClass`
- `NcssCount`
- `NPathComplexity`
- `SimplifyBooleanExpressions`
- `TooManyMethods`

PMD 7 removed the former `ExcessiveClassLength` and `ExcessiveMethodLength` rules; `NcssCount` is the supported size-oriented replacement used here.

### Analysis execution

- Final command: `.\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -DskipTests compile pmd:pmd`
- Result: **BUILD SUCCESS in 19.100 seconds**.
- Report: `target/pmd.xml` and `target/reports/pmd.html`.
- Analysed production source files: 41.
- Processing errors: 0.
- Initial unsuppressed findings: **32**.
- Priorities: **2 Priority 2**, **30 Priority 3**.
- Ruleset categories: **19 Best Practices**, **9 Design**, **3 Documentation**, **1 Error Prone**.
- Triage: **5 real maintainability issues**, **8 minor style issues**, **17 false positives/not applicable**, **2 needing further investigation**.

#### Tooling-resolution record

1. The first PMD invocation with bundled PMD 7.17.0 failed before producing a valid analysis because its ASM version could not read JDK 26 runtime classes.
2. PMD 7.27.0 with quickstart alone produced 23 findings, but inspection showed that quickstart excludes the requested complexity rules.
3. Listing individual design rule paths directly in the Maven plugin's `<rulesets>` block caused the plugin to copy and execute the whole design category repeatedly, producing an invalid 223-item duplicate report. That output was discarded.
4. A single checked-in ruleset referencing quickstart and the selected design rules exactly once produced the valid 32-finding report described here.

PMD warns that it is using the current JDK 26 `jrt-fs.jar` as the auxiliary classpath while source is targeted to Java 25. No JDK 25 is installed locally. This can affect type-sensitive rules in principle; the material complexity findings below are syntax/control-flow metrics and do not depend on JDK API-version inference. A future CI gate should run under the project's intended JDK 25 when available.

### Finding inventory

| ID | Rule | Location | Priority | Triage |
|---|---|---|---:|---|
| PMD-001 | `CloseResource` | `CampusSignalApplication.main`, line 11 | 3 | FALSE POSITIVE / NOT APPLICABLE |
| PMD-002 | `UncommentedEmptyConstructor` | `Email.Email`, line 32 | 3 | MINOR STYLE ISSUE |
| PMD-003 | `UncommentedEmptyConstructor` | `GmailWatchState.GmailWatchState`, line 30 | 3 | MINOR STYLE ISSUE |
| PMD-004 | `UncommentedEmptyConstructor` | `UserProfile.UserProfile`, line 45 | 3 | MINOR STYLE ISSUE |
| PMD-005 | `CognitiveComplexity` | `GmailApiClient.listMessageIds`, line 34 | 3 | REAL MAINTAINABILITY ISSUE |
| PMD-006 | `CyclomaticComplexity` | `GmailApiClient.listMessageIds`, line 34 | 3 | REAL MAINTAINABILITY ISSUE |
| PMD-007 | `LooseCoupling` | `GmailApiClient.listHistoryPage`, line 90 | 3 | FALSE POSITIVE / NOT APPLICABLE |
| PMD-008 | `PreserveStackTrace` | `GmailApiClient.listHistoryPage`, line 105 | 3 | NEEDS FURTHER INVESTIGATION |
| PMD-009 | `LooseCoupling` | `GmailAuthorizationService.createFlow`, line 32 | 3 | FALSE POSITIVE / NOT APPLICABLE |
| PMD-010 | `AvoidUsingHardCodedIP` | `GmailAuthorizationService.authorize`, line 56 | 3 | FALSE POSITIVE / NOT APPLICABLE |
| PMD-011 | `GuardLogStatement` | `GmailCommand.run`, lines 75–76 | 2 | FALSE POSITIVE / NOT APPLICABLE |
| PMD-012 | `LooseCoupling` | `GmailMessageMapper.map` (`Message`), line 20 | 3 | FALSE POSITIVE / NOT APPLICABLE |
| PMD-013 | `LooseCoupling` | `GmailMessageMapper.map` (`MessagePart`), line 24 | 3 | FALSE POSITIVE / NOT APPLICABLE |
| PMD-014 | `LooseCoupling` | `GmailMessageMapper.header`, line 35 | 3 | FALSE POSITIVE / NOT APPLICABLE |
| PMD-015 | `LooseCoupling` | `GmailMessageMapper.receivedAt`, line 63 | 3 | FALSE POSITIVE / NOT APPLICABLE |
| PMD-016 | `CyclomaticComplexity` | `GmailMessageMapper.collectText`, line 78 | 3 | NEEDS FURTHER INVESTIGATION |
| PMD-017 | `LooseCoupling` | `GmailMessageMapper.collectText`, line 78 | 3 | FALSE POSITIVE / NOT APPLICABLE |
| PMD-018 | `LooseCoupling` | `GmailMessageMapper.charset`, line 101 | 3 | FALSE POSITIVE / NOT APPLICABLE |
| PMD-019 | `CyclomaticComplexity` | `GmailNotificationDecoder.decode`, line 16 | 3 | REAL MAINTAINABILITY ISSUE |
| PMD-020 | `GuardLogStatement` | `PubSubPullSubscriber.dispatch`, line 45 | 2 | FALSE POSITIVE / NOT APPLICABLE |
| PMD-021 | `CyclomaticComplexity` | `CategoryScoreService.profileRelevance`, line 37 | 3 | MINOR STYLE ISSUE |
| PMD-022 | `LooseCoupling` | `GmailHistorySyncService.sync`, line 40 | 3 | FALSE POSITIVE / NOT APPLICABLE |
| PMD-023 | `LooseCoupling` | `GmailHistorySyncService.loadAllPages` return type, line 71 | 3 | FALSE POSITIVE / NOT APPLICABLE |
| PMD-024 | `LooseCoupling` | `GmailHistorySyncService.loadAllPages` local collection, line 73 | 3 | FALSE POSITIVE / NOT APPLICABLE |
| PMD-025 | `CognitiveComplexity` | `GmailHistorySyncService.messageAddedIds`, line 98 | 3 | REAL MAINTAINABILITY ISSUE |
| PMD-026 | `LooseCoupling` | `GmailHistorySyncService.messageAddedIds`, line 98 | 3 | FALSE POSITIVE / NOT APPLICABLE |
| PMD-027 | `GodClass` | `RuleBasedEmailClassificationService`, line 13 | 3 | FALSE POSITIVE / NOT APPLICABLE |
| PMD-028 | `TooManyMethods` | `RuleBasedEmailClassificationService`, line 13 | 3 | REAL MAINTAINABILITY ISSUE |
| PMD-029 | `CyclomaticComplexity` | `RuleBasedEmailClassificationService.classifyCurrent`, line 56 | 3 | MINOR STYLE ISSUE |
| PMD-030 | `LiteralsFirstInComparisons` | `isNegatedByNearbyWord`: comparison with `"not"`, line 329 | 3 | MINOR STYLE ISSUE |
| PMD-031 | `LiteralsFirstInComparisons` | `isNegatedByNearbyWord`: comparison with `"no"`, line 329 | 3 | MINOR STYLE ISSUE |
| PMD-032 | `LiteralsFirstInComparisons` | `isNegatedByNearbyWord`: comparison with `"non"`, line 329 | 3 | MINOR STYLE ISSUE |

## Detailed triage

### PMD-005 and PMD-006 — `GmailApiClient.listMessageIds`

1. **Rules:** `CognitiveComplexity` measures how difficult nested/structured control flow is to follow; `CyclomaticComplexity` counts independent control-flow paths. PMD reports cognitive complexity 21 (threshold 15) and cyclomatic complexity 15.
2. **Affected code:** limit validation, paginated Gmail requests, optional response-list handling, ID filtering/deduplication, maximum truncation, repeated-token protection, and the loop condition are all in `listMessageIds`.
3. **Trigger:** one method owns input validation, page fetching, response normalization, pagination safety, and termination decisions.
4. **Judgement:** **REAL MAINTAINABILITY ISSUE**. The method is not exceptionally long, but its multiple concerns make edge-case review harder.
5. **Behavior risk:** medium/high. Pagination boundaries, duplicate protection, provider nulls, and maximum result semantics are externally observable Gmail behavior.
6. **Smallest sensible refactoring:** preserve the loop and extract only named helpers for limit validation and adding usable message IDs from one response page. Do not replace the logic with a generic pagination framework.
7. **Current test protection:** `GmailApiClientTest.paginatesInboxRequestsAndHonorsMaximum`, `emptyInboxAndInvalidLimitsAreHandled`, and `providerErrorsPropagateAndRepeatedPageTokensAreRejected`.
8. **Additional tests before refactoring:** duplicate IDs across pages, null/blank provider IDs, blank final page token, and a page containing more messages than the remaining maximum.
9. **Runtime note:** these findings describe control-flow maintainability, not Big-O complexity or a demonstrated performance problem.

### PMD-019 — `GmailNotificationDecoder.decode`

1. **Rule:** `CyclomaticComplexity`; PMD reports 18 control-flow paths.
2. **Affected code:** JSON parsing, root-shape validation, email validation, and numeric/string history-ID validation share one method.
3. **Trigger:** compound null/type/value checks and the two accepted history-ID representations contribute separate paths.
4. **Judgement:** **REAL MAINTAINABILITY ISSUE**. Each validation is understandable, but the combined method obscures the three independent contracts.
5. **Behavior risk:** medium/high because notification acceptance determines ack/nack and synchronization behavior.
6. **Smallest sensible refactoring:** extract `requiredEmailAddress(root)` and `requiredHistoryId(root)` while preserving exact exception types and sanitized messages.
7. **Current test protection:** both `GmailNotificationDecoderTest` tests cover accepted string/numeric IDs and several malformed/incomplete inputs; Pub/Sub tests cover failure nack behavior.
8. **Additional tests before refactoring:** explicit null JSON, non-object JSON, blank email, negative numeric history ID, non-digit string, and unsupported history-ID node type as individually named cases.
9. **Runtime note:** fewer branches may improve reviewability but does not imply better algorithmic time complexity.

### PMD-025 — `GmailHistorySyncService.messageAddedIds`

1. **Rule:** `CognitiveComplexity`; PMD reports 16 against a threshold of 15.
2. **Affected code:** three nested provider-model loops plus null and blank filtering.
3. **Trigger:** the nested Gmail `page → history record → message-added entry → message` structure and defensive checks increase nesting cost.
4. **Judgement:** **REAL MAINTAINABILITY ISSUE**, although only slightly over threshold. The provider hierarchy is inherently nested, but extracting one level would make the filtering contract easier to inspect.
5. **Behavior risk:** medium. Missing provider collections, duplicate message IDs, and blank IDs must remain harmless and deterministic.
6. **Smallest sensible refactoring:** retain imperative iteration and extract a helper that adds valid IDs from one history record or one page. Avoid a dense null-heavy stream pipeline.
7. **Current test protection:** `GmailRealtimeServiceTest.historyIngestsAddedMessagesAndAdvancesOnlyAfterSuccess` covers ordinary extraction and deduplicated persistence indirectly; the five realtime tests protect progress and recovery behavior.
8. **Additional tests before refactoring:** null page history, null `messagesAdded`, null message, blank ID, and duplicates across records/pages.
9. **Runtime note:** this is nested data traversal, not evidence of poor asymptotic performance; every provider element is visited once.

### PMD-028 — method count in `RuleBasedEmailClassificationService`

1. **Rule:** `TooManyMethods` reports classes whose method count exceeds the configured default threshold.
2. **Affected code:** the class combines orchestration, category evidence rules, phrase matching, negation detection, quote/footer splitting, and text normalization.
3. **Trigger:** accumulated category-specific evidence and preprocessing helpers place many private methods in one class.
4. **Judgement:** **REAL MAINTAINABILITY ISSUE**. The class still has one business purpose, but navigation and safe ownership of future rule changes are becoming harder.
5. **Behavior risk:** high. Method ordering in `classifyCurrent` encodes precedence; normalization and negation decisions influence all categories.
6. **Smallest sensible refactoring:** first extract stateless text preprocessing/negation operations into one package-private collaborator. Keep category evidence methods and precedence orchestration together until evidence supports another split.
7. **Current test protection:** all 40 `RuleBasedEmailClassificationServiceTest` tests, covering categories, precedence, quote rescue, normalization, negation, ties, HTML, feedback, and confidence.
8. **Additional tests before refactoring:** focused characterization for repeated reply/forward prefixes, quote-boundary variants, exact four-token negation-window edges, footer/signature variants, and direct preprocessing invariants.
9. **Runtime note:** splitting responsibilities improves comprehensibility; it does not automatically change runtime complexity.

### PMD-027 — `GodClass` in `RuleBasedEmailClassificationService`

1. **Rule:** `GodClass` combines weighted method complexity, access to foreign data, and low method cohesion. PMD reports WMC 70, ATFD 7, and TCC 0.000%.
2. **Affected code:** the entire stateless rule-based classifier.
3. **Trigger:** numerous helpers share parameters and call other helpers rather than shared instance fields, so field-based cohesion appears near zero; DTO accessors count as foreign data.
4. **Judgement:** **FALSE POSITIVE / NOT APPLICABLE as a “God Class.”** The class has one cohesive responsibility—deterministic classification—and does not centralize unrelated application subsystems. The separate `TooManyMethods` result is still a valid maintainability signal.
5. **Behavior risk:** high if the label motivates a broad redesign.
6. **Smallest sensible action:** do not refactor to satisfy the GodClass metric. Use the narrower method-count finding to guide one bounded extraction.
7. **Current test protection:** 40 classifier tests.
8. **Additional tests:** the preprocessing/negation characterization listed for PMD-028 should precede any extraction.

### PMD-029 — `classifyCurrent` cyclomatic complexity

1. **Rule:** `CyclomaticComplexity`; PMD reports 10, exactly at the default reporting threshold.
2. **Affected code:** the ordered sequence of category qualification gates and the academic/event tie decision.
3. **Trigger:** each category gate contributes a decision path.
4. **Judgement:** **MINOR STYLE ISSUE**. The method is a readable linear decision chain, and its order directly documents business precedence. PMD reports no cognitive or NPath violation for it.
5. **Behavior risk:** high; converting it to a generic table/strategy could obscure or accidentally reorder precedence.
6. **Smallest sensible action:** leave the method intact during the first structural extraction. Reassess only if more categories materially lengthen it.
7. **Current test protection:** 40 classifier tests, especially `careerVsEvent`, `academicVsEvent`, `callVsEvent`, `exactTieFallsBackToGeneral`, and `wellbeingServiceEventIsWellbeing`.
8. **Additional tests:** no test is required merely to leave this method unchanged; any future precedence rewrite requires a dedicated pairwise precedence matrix.

### PMD-016 — `GmailMessageMapper.collectText`

1. **Rule:** `CyclomaticComplexity`; PMD reports 12.
2. **Affected code:** null/attachment filtering, MIME/body validation, plain-versus-HTML handling, and recursive multipart traversal.
3. **Trigger:** compound guards and format branches share a recursive method.
4. **Judgement:** **NEEDS FURTHER INVESTIGATION**. The method is compact and follows the MIME tree directly, but further format support could make it brittle.
5. **Behavior risk:** high because a refactor can change attachment exclusion, plain-text precedence, HTML sanitization, charset decoding, or traversal order.
6. **Smallest possible refactoring:** extract a pure `isAttachment(part)` predicate or body-decoding helper only if added characterization shows a stable seam.
7. **Current test protection:** six `GmailMessageMapperTest` tests cover multipart/plain preference, attachments, HTML, charset, fallbacks, and malformed input.
8. **Additional tests:** nested multipart alternatives with multiple plain/html parts, filename-only versus disposition-only attachments, null child lists, and traversal order.

### PMD-008 — stale-history exception translation

1. **Rule:** `PreserveStackTrace` warns when a caught exception is replaced without retaining the cause on every path.
2. **Affected code:** Gmail HTTP 404 is translated from `GoogleJsonResponseException` to `StaleGmailHistoryException`.
3. **Trigger:** the domain exception constructor has no cause, while non-404 provider exceptions are rethrown unchanged.
4. **Judgement:** **NEEDS FURTHER INVESTIGATION**. Preserving a cause would improve diagnostics, but provider exceptions may contain private response details and the stale-history path is an expected recovery signal.
5. **Behavior risk:** medium. A retained cause could later be logged by generic infrastructure and expose provider detail.
6. **Smallest possible refactoring:** add a cause-taking domain constructor only after verifying every logging/user-output boundary remains sanitized.
7. **Current test protection:** `GmailApiClientTest.staleHistoryIsDistinguishedFromOtherProviderErrors`, `GmailRealtimeServiceTest.staleHistoryRunsBoundedRecoveryAndResetsProgress`, and command privacy tests.
8. **Additional tests:** assert that a preserved cause remains absent from command/PubSub user-visible output and that stale recovery still runs.

### PMD-021 — `CategoryScoreService.profileRelevance`

1. **Rule:** `CyclomaticComplexity`; PMD reports 11.
2. **Affected code:** a three-case switch with short boolean membership checks.
3. **Trigger:** PMD counts each `||` term in the research and hackathon cases.
4. **Judgement:** **MINOR STYLE ISSUE**. The current method is short, explicit, and easier to audit than an indirect strategy/data structure for three categories.
5. **Behavior risk:** medium because changes alter scoring.
6. **Smallest sensible action:** no refactor now; extract named predicates only if category logic grows.
7. **Current test protection:** 11 `CategoryScoreServiceTest` executions cover normalization, components, weights, user differences, and programme/department/year relevance.
8. **Additional tests:** unnecessary unless new categories or predicates are introduced.

### PMD-002, PMD-003, and PMD-004 — JPA constructors

1. **Rule:** `UncommentedEmptyConstructor` asks for documentation explaining intentionally empty constructors.
2. **Affected code:** protected no-argument constructors in `Email`, `GmailWatchState`, and `UserProfile`.
3. **Trigger:** each constructor is intentionally empty and has no adjacent explanatory comment.
4. **Judgement:** **MINOR STYLE ISSUE**. A short “Required by JPA” comment would help readers, but behavior is correct.
5. **Behavior risk:** negligible if only comments are added; changing visibility or removing constructors would be risky.
6. **Smallest sensible remediation:** add one concise JPA-purpose comment to each constructor in a later cleanup.
7. **Current test protection:** `EmailPersistenceTest` (6 tests), `GmailRealtimeServiceTest` (5), and `ProfileApiTest` (26).
8. **Additional tests:** none for comment-only changes.

### PMD-030, PMD-031, and PMD-032 — literal-first string comparisons

1. **Rule:** `LiteralsFirstInComparisons` prefers `"literal".equals(value)` for null safety.
2. **Affected code:** the three comparisons against `words[index]` in `isNegatedByNearbyWord`.
3. **Trigger:** the array value is the receiver of `.equals`.
4. **Judgement:** **MINOR STYLE ISSUE**. Values come from `String.split` and cannot be null, so this is not a current defect.
5. **Behavior risk:** low, but the negation boundary is behavior-sensitive.
6. **Smallest sensible remediation:** mechanically reverse the three comparisons in a later focused cleanup.
7. **Current test protection:** classifier negation tests and the full 40-test classifier suite.
8. **Additional tests:** no extra tests for the mechanical swap; exact negation-window characterization is still recommended before the larger extraction.

### PMD-007, PMD-009, PMD-012–015, PMD-017–018, and PMD-022–024, PMD-026 — `LooseCoupling`

1. **Rule:** `LooseCoupling` recommends declaring variables and boundaries with interfaces rather than concrete implementation types.
2. **Affected code:** Google's generated API model types `ListHistoryResponse`, `GoogleClientSecrets`, `Message`, and `MessagePart`, including lists of history responses.
3. **Trigger:** PMD recognizes these concrete collection/model-like classes but has no provider-neutral interface to recommend.
4. **Judgement:** **FALSE POSITIVE / NOT APPLICABLE** for all 12 findings. These are the types required by the Gmail client library; most have no useful interface. Provider types remain inside the Gmail adapter/history integration and do not cross the core `EmailSource` boundary.
5. **Behavior risk:** high if changed merely to satisfy PMD; replacing them with `Object` or artificial wrappers would weaken type safety or add unnecessary mapping.
6. **Smallest sensible action:** no production change and no blanket suppression during this phase. Consider narrow per-rule exclusions only after team approval if a future gate requires them.
7. **Current test protection:** Gmail API (9), authorization (4), mapper (6), and realtime synchronization (5) tests.
8. **Additional tests:** none because no refactoring is proposed.

### PMD-001 — Spring application context lifetime

1. **Rule:** `CloseResource` identifies closeable resources that appear not to be closed.
2. **Affected code:** the `ConfigurableApplicationContext` returned from `SpringApplication.run`.
3. **Trigger:** the context is assigned locally and remains open on the normal web-server path.
4. **Judgement:** **FALSE POSITIVE / NOT APPLICABLE**. A Spring Boot service must retain its application context for the process lifetime; try-with-resources would shut down the application immediately. The command path explicitly delegates shutdown through `SpringApplication.exit`.
5. **Behavior risk:** critical if changed blindly.
6. **Smallest sensible action:** none.
7. **Current test protection:** Spring context-loading integration tests exist, but `main` process lifetime is not directly tested.
8. **Additional tests:** a process-level startup/shutdown test would be required before altering lifecycle code, but no alteration is justified.

### PMD-010 — OAuth loopback address

1. **Rule:** `AvoidUsingHardCodedIP` generally treats embedded IP literals as environment-specific configuration.
2. **Affected code:** `LocalServerReceiver.Builder().setHost("127.0.0.1")`.
3. **Trigger:** a literal IPv4 address appears in production source.
4. **Judgement:** **FALSE POSITIVE / NOT APPLICABLE**. Loopback binding is an intentional OAuth privacy/security boundary, not a deployable remote endpoint.
5. **Behavior risk:** high if made externally configurable without an explicit security decision.
6. **Smallest sensible action:** keep the explicit loopback address.
7. **Current test protection:** authorization tests cover scope, offline access, credential reuse, and safe missing-file failure; they do not open the interactive receiver.
8. **Additional tests:** only needed if callback binding policy changes.

### PMD-011 and PMD-020 — guarded logging

1. **Rule:** `GuardLogStatement` recommends checking the log level before parameterized logging when argument construction may be costly.
2. **Affected code:** one `ERROR` event in `GmailCommand` and one in `PubSubPullSubscriber`.
3. **Trigger:** parameterized logging is not wrapped in `LOGGER.isErrorEnabled()`.
4. **Judgement:** **FALSE POSITIVE / NOT APPLICABLE**. Arguments are existing strings and `getSimpleName()`, with negligible evaluation cost; an extra guard would add noise. Both events are intentional failure diagnostics and omit sensitive messages/payloads.
5. **Behavior risk:** low mechanically, but logging changes must preserve privacy guarantees.
6. **Smallest sensible action:** none.
7. **Current test protection:** 7 command tests and 3 Pub/Sub tests, including sanitized runtime logging and ack/nack behavior.
8. **Additional tests:** none without a logging-policy change.

## Negative complexity/code-smell results

The configured analysis reported no findings for:

- `NPathComplexity`
- `NcssCount` at method or class level
- `AvoidDeeplyNestedIfStmts`
- `CollapsibleIfStatements`
- `ExcessiveParameterList`
- `SimplifyBooleanExpressions`
- unused local variables, private fields, or private methods
- empty catches or empty control blocks
- identical catch branches
- unnecessary local-variable inlining
- PMD performance-category rules included by quickstart

This means the classifier's size is signaled by method count and aggregate class metrics, not by an individually excessive method, excessive NCSS, deep nesting, high NPath, or high cognitive-complexity finding.

## Complexity versus algorithmic performance

The reported cyclomatic and cognitive scores are control-flow/readability metrics. Lowering those scores does not inherently improve Big-O runtime. No genuinely performance-sensitive algorithm or PMD performance finding was identified in this task. The classifier performs repeated fixed-rule phrase/regex checks; profiling should precede any optimization if future production volume makes throughput a concern.

## Proposed remediation order

1. **Classifier structural extraction:** after adding focused preprocessing/negation characterization, extract only normalization, quote/footer splitting, and negation matching into one package-private collaborator. Preserve category rule methods and `classifyCurrent` precedence exactly.
2. **Gmail boundary helpers:** independently characterize and then extract validation/collection helpers from `GmailNotificationDecoder.decode`, `GmailApiClient.listMessageIds`, and `GmailHistorySyncService.messageAddedIds`.
3. **Investigate before changing:** decide whether retaining the original stale-history provider exception as a cause is compatible with privacy-safe logging; decide whether `collectText` is clearer after a small predicate extraction.
4. **Optional style cleanup:** JPA constructor comments and literal-first negation comparisons can be handled separately after behavior-sensitive work.
5. **False positives:** leave the 17 not-applicable findings unsuppressed until a separate, explicitly approved narrow-suppression/gating decision.

## Unresolved questions

- Should classifier preprocessing become a package-private collaborator or remain private static helpers to minimize API surface?
- Which direct preprocessing/negation tests are required before extraction, beyond the existing 40 end-to-end classifier cases?
- May `StaleGmailHistoryException` retain the provider exception cause internally without risking future leakage through logs?
- Should a future PMD CI gate run exclusively on JDK 25 to remove the current JDK 26 auxiliary-classpath warning?
- Does the course/team require zero PMD findings, or will reviewed false positives remain visible as with SpotBugs?

## Regression verification

Post-tooling/documentation command: `.\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -Ddebug=false test`.

Result: **BUILD SUCCESS — 122 tests run, 0 failures, 0 errors, 0 skipped in 29.183 seconds**.

## Exact next recommended step

Create a separate test-first remediation task for the classifier's `TooManyMethods` finding: add focused quote-splitting, repeated-prefix, normalization, and four-token negation-boundary characterization tests; then extract only text preprocessing/negation into one package-private collaborator, preserve `classifyCurrent` order and every existing expectation, run all 40 classifier tests plus the full Java suite, and rerun PMD to measure the structural change.

---

## 2026-10-06 — Test-first remediation results

### Preserved baseline

- Branch: `phase4-live-demo`.
- Commit before changes: `71e43ca00329ff056e17fca95ed8497b9da9d689`.
- Starting worktree: clean (`git status --short --branch` reported only `## phase4-live-demo`).
- Baseline command: `.\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" clean test`.
- Baseline result: **BUILD SUCCESS — 122 tests run, 0 failures, 0 errors, 0 skipped in 40.922 seconds**.
- PMD baseline command: `.\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -DskipTests compile pmd:pmd`.
- PMD baseline: **32 findings across 13 classes**, reproducing the initial analysis. Plugin `3.28.0`, PMD `7.27.0`, and the existing `config/pmd/ruleset.xml` were unchanged.

### PMD-005 and PMD-006 — `GmailApiClient.listMessageIds`

- **Before:** cognitive complexity 21; cyclomatic complexity 15.
- **Characterization added:** `ignoresMissingBlankAndDuplicateIdsAcrossPages` and `blankNextPageTokenEndsPagination`. Existing tests already covered ordinary pagination, empty/missing message lists, maximum enforcement, request parameters, provider failures, repeated tokens, and invalid limits.
- **Refactoring:** retained one request per page and the same remaining-result limit while extracting bounded ID collection and next-token validation. Invalid/blank IDs remain ignored, insertion order and deduplication remain stable, blank tokens stop pagination, and repeated non-null tokens still fail.
- **After:** neither cognitive nor cyclomatic rule reports `listMessageIds`; therefore cognitive complexity is below the configured reporting threshold of 15 and cyclomatic complexity is below 10. Both findings disappeared.
- **Verification:** 11 focused tests passed; the complete suite passed with 124 tests; PMD fell from 32 to 30 findings.

### PMD-019 — `GmailNotificationDecoder.decode`

- **Before:** cyclomatic complexity 18.
- **Characterization added:** `rejectsBlankJsonDocument`, `rejectsMissingRequiredFieldsWithTheirExistingMessages`, and `rejectsNonNumericTextualHistoryId`. Existing tests continued to cover valid trimmed email addresses, string and integral history IDs, malformed JSON, non-object JSON, blank email, unsupported history types, and negative numeric IDs.
- **Base64 boundary:** malformed Base64 is not an input state of this decoder. The Google Pub/Sub client supplies already-decoded `ByteString` bytes; malformed decoded payload bytes enter as invalid JSON and are rejected/nacked by the existing decoder/subscriber path. Null byte arrays are not a supported Pub/Sub state because `PubsubMessage.getData()` is non-null.
- **Refactoring:** separated JSON-object parsing, required email extraction, and required history-ID extraction without changing accepted formats, exception types, or sanitized messages.
- **After:** `decode` has no cyclomatic finding (below 10); the finding disappeared.
- **Verification:** 5 focused tests passed; the complete suite passed with 127 tests; PMD fell from 30 to 29 findings.

### PMD-025 — `GmailHistorySyncService.messageAddedIds`

- **Before:** cognitive complexity 16.
- **Characterization added:** `emptyHistoryAdvancesProgressWithoutFetchingMessages` and `ignoresMissingNestedMessagesAndDeduplicatesIdsAcrossPages`.
- **Refactoring:** expressed the provider hierarchy as ordered page/history/message-added traversal, converting missing collections to empty streams, filtering null messages and null/blank IDs, and retaining first-seen encounter order through `distinct()`.
- **After:** no cognitive-complexity finding remains (below 15); the finding disappeared.
- **Verification:** 7 focused tests passed; the complete suite passed with 129 tests; PMD fell from 29 to 28 findings.

### PMD-028 — `RuleBasedEmailClassificationService` method count

- **Before:** `TooManyMethods` remained visible; source inspection counted 25 declared outer-class methods. The separate `GodClass` report was already triaged not applicable.
- **Characterization added:** `repeatedPrefixesAndUnicodeSpacingNormalizeTogether`, `forwardedMessageBoundaryKeepsQuotedEvidenceOutOfCurrentMessage`, and `nearbyNegationWindowIncludesExactlyFourPrecedingTokens`. The existing filtering tests for signature, footer, URL, and email-address text were retained unchanged.
- **Refactoring:** moved only stateless text normalization, URL/email/punctuation filtering, quote/footer segmentation, phrase matching, and nearby-negation scanning into package-private `ClassificationTextPreprocessor`. Category evidence lists, scores, confidence mapping, tie behavior, and `classifyCurrent` precedence remain in the classifier and were not changed.
- **After:** the classifier has 14 declared outer-class methods. `TooManyMethods` still reports because the configured default threshold is 10. The preprocessing collaborator does not report `TooManyMethods`; the previously not-applicable `GodClass` signal also disappeared. The remaining classifier methods are cohesive orchestration and category-evidence rules, so further splitting in this task would be metric-driven rather than evidence-driven.
- **Verification:** all 43 classifier tests passed, including the unchanged original 40; the complete suite passed with 132 tests. PMD settled at 27 findings after collaborator consolidation.

### PMD-016 investigation — `GmailMessageMapper.collectText`

- **Decision:** leave unchanged. Its cyclomatic complexity remains 12.
- **Reasoning:** the method is a compact recursive traversal of an inherently branched MIME tree. Its branches directly express attachment exclusion, body availability, plain-versus-HTML decoding, and child traversal. Splitting it now would risk altering plain-text preference, traversal order, charset fallback, HTML cleanup, or attachment behavior without a clearer ownership seam.
- **Existing protection:** six mapper tests cover nested multipart/plain preference, filename attachments, HTML fallback and cleanup, UTF-8/declared charset decoding, missing payload/optional headers, and malformed encoded body data.
- **Recommended characterization before any future refactor:** disposition-only attachments, multiple same-type body fragments and traversal order, null/empty child bodies, and invalid/unknown charset fallback.

### PMD-008 investigation — stale-history cause preservation

- **Decision:** leave `StaleGmailHistoryException` without the underlying `GoogleJsonResponseException` cause. `PreserveStackTrace` remains visible.
- **Diagnosability:** the domain exception type already identifies the expected Gmail 404/stale-cursor condition and triggers bounded recovery; non-404 provider failures are rethrown unchanged.
- **Privacy:** a retained provider exception may carry response text or request/provider metadata and could later be emitted by generic stack-trace logging. The stale path is an expected recovery signal, so that additional detail does not currently justify the exposure risk.
- **External behavior:** unchanged. Recovery, user-visible sanitization, and command/PubSub behavior remain the same.

### Final PMD comparison

- **Before:** 32 findings — 19 Best Practices, 9 Design, 3 Documentation, 1 Error Prone; Priority 2: 2, Priority 3: 30.
- **After:** 27 findings across 13 classes — 19 Best Practices, 4 Design, 3 Documentation, 1 Error Prone; Priority 2: 2, Priority 3: 25.
- **Disappeared:** the two `listMessageIds` complexity findings, decoder cyclomatic complexity, history traversal cognitive complexity, and the not-applicable classifier `GodClass` signal.
- **Remaining rule totals:** 12 `LooseCoupling`, 3 `CyclomaticComplexity`, 3 `LiteralsFirstInComparisons`, 3 `UncommentedEmptyConstructor`, 2 `GuardLogStatement`, and one each of `AvoidUsingHardCodedIP`, `CloseResource`, `PreserveStackTrace`, and `TooManyMethods`.
- No suppression, exclusion, rule threshold, PMD configuration, SpotBugs configuration, or production behavior contract was changed.

### Final verification and next step

- Final focused results: Gmail API 11/11, decoder 5/5, realtime history 7/7, classifier 43/43.
- Final clean command: `.\mvnw.cmd -B -ntp clean test`.
- Final clean Java result: **BUILD SUCCESS — 132 tests run, 0 failures, 0 errors, 0 skipped in 37.600 seconds**.
- Final PMD rerun: **BUILD SUCCESS — 27 findings across 13 classes**.
- No genuine behavioral defect was discovered; `docs/BUG_LOG.md` was not changed.
- Exact next recommended step: add the four mapper characterizations listed above in a separate test-first task, then reassess whether a single attachment/body-decoding predicate extraction materially improves `collectText`; keep stale-history cause preservation unchanged unless a privacy-reviewed internal diagnostic sink is introduced.
