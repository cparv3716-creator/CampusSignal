# Static Analysis Log

## 2026-10-06 — Initial SpotBugs analysis and triage

Status: **INITIAL TRIAGE COMPLETE — NO PRODUCTION FIXES APPLIED**

### Environment and baseline

- Branch: `phase4-live-demo`
- Commit: `cb7bb4f97869a725444bbca5d3751bd22439f953`
- Starting working tree: clean
- Project Java target: 25
- Analysis runtime: Java `26.0.2` (`26.0.2+10-55`)
- SpotBugs Maven plugin: `4.10.4.1`
- SpotBugs analysis engine reported in XML: `4.10.4`
- Configuration: `effort=Max`, `threshold=Low`, XML output enabled, no include/exclude filter, no suppression file
- Baseline command: `.\\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -Ddebug=false test`
- Baseline result: BUILD SUCCESS; 117 tests run, 0 failures, 0 errors, 0 skipped in 32.341 seconds

### Reproducible analysis command

`.\\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -DskipTests compile spotbugs:spotbugs`

Result: BUILD SUCCESS. The unsuppressed XML reports were generated at `target/spotbugsXml.xml` and `target/spotbugs.xml`.

Official references:

- [SpotBugs Maven plugin documentation](https://spotbugs.github.io/spotbugs-maven-plugin/)
- [SpotBugs 4.10.4 bug descriptions](https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html)

### Initial findings summary

- Total findings: **6** across 50 analysed classes
- SpotBugs categories: 2 `MALICIOUS_CODE`, 3 `STYLE`, 1 `BAD_PRACTICE`
- Priorities: 1 Priority 1, 4 Priority 2, 1 Priority 3
- Triage: 0 confirmed runtime defects, 3 code-quality/maintainability issues, 2 false positives, 1 needs further investigation
- No finding was hidden, filtered, suppressed, or fixed during this initial analysis.

| ID | SpotBugs type | Priority / rank | Class and location | Triage | Proposed disposition |
|---|---|---:|---|---|---|
| SA-001 | `EI_EXPOSE_REP` | P2 / 18 | `ProfileCreateRequest.interests()`, line 7 | FALSE POSITIVE | Verify immutability with a focused test; later justify a narrow suppression/filter if a clean report is required. |
| SA-002 | `DCN_NULLPOINTER_EXCEPTION` | P2 / 17 | `GmailApiClient.historyId(String)`, line 126 | CODE QUALITY / MAINTAINABILITY | Replace NPE catching with an explicit null check and catch only `NumberFormatException`. |
| SA-003 | `REC_CATCH_EXCEPTION` | P1 / 15 | `GmailCommand.run(ApplicationArguments)`, line 66 | NEEDS FURTHER INVESTIGATION | Decide whether the CLI privacy/error boundary should intentionally catch runtime failures or expose programmer defects. |
| SA-004 | `EI_EXPOSE_REP2` | P2 / 18 | `GmailNotificationDecoder(ObjectMapper)`, line 13 | FALSE POSITIVE | Retain trusted Spring constructor injection; only isolate with `ObjectMapper.copy()` if configuration ownership requires it. |
| SA-005 | `THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION` | P3 / 19 | `PubSubPullSubscriber.Handler.handle(byte[])`, line 18 | CODE QUALITY / MAINTAINABILITY | Consider narrowing the checked contract to `IOException` or a domain processing exception. |
| SA-006 | `DCN_NULLPOINTER_EXCEPTION` | P2 / 17 | `GmailHistorySyncService.historyId(String)`, line 126 | CODE QUALITY / MAINTAINABILITY | Replace NPE catching with an explicit null check and catch only `NumberFormatException`. |

## Finding triage

### SA-001 — `EI_EXPOSE_REP` in `ProfileCreateRequest`

1. **Rule:** `EI_EXPOSE_REP` reports an accessor that may return a mutable object held as internal state.
2. **General meaning:** callers could mutate the returned object and silently change the owning object's state.
3. **CampusSignal trigger:** the generated record accessor `ProfileCreateRequest.interests()` returns the record's `List<String>` field.
4. **Judgement:** **FALSE POSITIVE**. The compact constructor replaces non-null input with `interests.stream().map(...).distinct().toList()`. Java's `Stream.toList()` result is unmodifiable, and its `String` elements are immutable. The original caller list is not retained.
5. **Possible runtime behaviour if real:** request interests could change after validation, creating validation/persistence inconsistency. That mutation path is not available with the current unmodifiable list.
6. **Smallest reasonable action:** do not change production code now. Add an immutability test, then use a narrow documented suppression/filter only if a clean future SpotBugs gate requires it. Overriding the accessor to copy on every call would be redundant.
7. **Existing test coverage:** `ProfileApiTest` verifies normalization, deduplication, validation, persistence, and response content, but not mutation resistance of the record accessor.
8. **Regression-test implication:** a small DTO unit test asserting that `request.interests().add(...)` throws `UnsupportedOperationException` would document why this finding is false.

### SA-002 — `DCN_NULLPOINTER_EXCEPTION` in `GmailApiClient.historyId`

1. **Rule:** `DCN_NULLPOINTER_EXCEPTION` flags catching `NullPointerException` instead of testing null explicitly.
2. **General meaning:** catching NPE can hide an unrelated programming error inside the protected block and makes input validation less explicit.
3. **CampusSignal trigger:** `historyId(String)` catches `NullPointerException | NumberFormatException` around `new BigInteger(value)` and the negative-value check.
4. **Judgement:** **CODE QUALITY / MAINTAINABILITY ISSUE**, not a currently observed runtime defect. The method intentionally converts null and malformed history IDs into `IllegalArgumentException`, but NPE catching is broader than required.
5. **Possible runtime behaviour if real:** a future unrelated NPE introduced inside the try block could be mislabeled as an invalid history ID, hiding its actual cause.
6. **Smallest reasonable fix:** check `value == null` first, then catch only `NumberFormatException`; preserve the existing `IllegalArgumentException` message and negative-value behaviour.
7. **Existing test coverage:** `GmailApiClientTest` covers valid history IDs and provider-error paths, but not the package-private parser's null, malformed, or negative input directly.
8. **Regression-test implication:** add focused package-level tests for null, non-numeric, negative, and valid history IDs before changing the method.

### SA-003 — `REC_CATCH_EXCEPTION` in `GmailCommand.run`

1. **Rule:** `REC_CATCH_EXCEPTION` flags a broad `catch (Exception)` where specific checked exceptions could be caught and unexpected runtime exceptions may be masked.
2. **General meaning:** broad catches can turn programmer errors into generic operational failures and make diagnosis difficult.
3. **CampusSignal trigger:** the top-level Gmail CLI runner catches every `Exception`, prints one sanitized message, and sets exit code 1.
4. **Judgement:** **NEEDS FURTHER INVESTIGATION**. The warning is technically accurate, but this is an intentional command boundary designed not to expose provider messages, credential paths, payloads, or email content. Whether programmer/runtime failures should also be sanitized is a product/operations policy decision.
5. **Possible runtime behaviour if real:** an unexpected `RuntimeException` is hidden behind a generic failure, reducing diagnosability; conversely, rethrowing it could leak sensitive details through Spring's startup logs.
6. **Smallest reasonable action:** decide the command-boundary policy first. Options are (a) retain and document the intentional broad boundary, possibly with a narrow justified suppression later, or (b) split known checked exceptions from runtime exceptions while preserving sanitized output and safe diagnostics.
7. **Existing test coverage:** `GmailCommandTest.errorsAreReportedWithoutProviderDetails` verifies checked `IOException` sanitization and exit code 1. No test defines the desired treatment of unexpected runtime exceptions.
8. **Regression-test implication:** add a runtime-failure test only after the policy decision, asserting both exit behavior and that private exception details never appear.

### SA-004 — `EI_EXPOSE_REP2` in `GmailNotificationDecoder`

1. **Rule:** `EI_EXPOSE_REP2` reports storing a caller-supplied mutable object as internal state.
2. **General meaning:** the caller could mutate that object later and change the receiving object's behaviour.
3. **CampusSignal trigger:** Spring injects an `ObjectMapper`, and the decoder stores the same reference in its final `mapper` field.
4. **Judgement:** **FALSE POSITIVE in this architecture**. Shared constructor-injected `ObjectMapper` use is intentional Spring infrastructure; the collaborator is trusted and configured by the application before normal request processing. The decoder does not expose it.
5. **Possible runtime behaviour if real:** external reconfiguration after construction could change accepted JSON or modules and make notification decoding inconsistent.
6. **Smallest reasonable action:** retain constructor injection. `mapper.copy()` is possible if isolated immutable configuration becomes a requirement, but it may diverge from application-managed modules and should not be introduced solely for this warning.
7. **Existing test coverage:** `GmailNotificationDecoderTest` covers valid string/numeric history IDs and malformed or incomplete notifications, but not post-construction mapper mutation.
8. **Regression-test implication:** no new test is necessary unless the project adopts a requirement that decoder configuration be isolated from the Spring mapper.

### SA-005 — `THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION` in `PubSubPullSubscriber.Handler`

1. **Rule:** this pattern flags a method declaring the broad checked type `Exception` instead of a more specific exception.
2. **General meaning:** callers and implementations lose information about expected failure modes and are forced into overly broad handling.
3. **CampusSignal trigger:** `Handler.handle(byte[]) throws Exception`; `dispatch` catches `Exception` and nacks the Pub/Sub message.
4. **Judgement:** **CODE QUALITY / MAINTAINABILITY ISSUE** with low immediate risk. The broad contract is used so the listener lambda may propagate `IOException` from history synchronization, and all handler failures intentionally cause nack.
5. **Possible runtime behaviour if real:** unrelated checked failures can be admitted without review, and operational logs retain only the exception class, making failure contracts less clear.
6. **Smallest reasonable fix:** if current handlers are authoritative, narrow the checked contract to `IOException` (runtime exceptions remain unchecked) or introduce a small domain-specific processing exception; preserve nack-on-failure behaviour.
7. **Existing test coverage:** `PubSubPullSubscriberTest` verifies ack after success and nack for an `IllegalStateException`, but not a checked `IOException`.
8. **Regression-test implication:** add a checked-I/O failure test that verifies nack and no ack before narrowing the interface.

### SA-006 — `DCN_NULLPOINTER_EXCEPTION` in `GmailHistorySyncService.historyId`

1. **Rule:** `DCN_NULLPOINTER_EXCEPTION`, the same pattern as SA-002.
2. **General meaning:** explicit null validation is preferable because catching NPE may mask unrelated defects.
3. **CampusSignal trigger:** the service's private history-ID parser catches `NullPointerException | NumberFormatException` while converting notification/stored IDs to `BigInteger`.
4. **Judgement:** **CODE QUALITY / MAINTAINABILITY ISSUE**, not a currently observed runtime defect. Current invalid inputs are deliberately translated to `IllegalArgumentException`.
5. **Possible runtime behaviour if real:** a future NPE within the try block could be reported as malformed Gmail history data, obscuring the actual bug and possibly causing repeated Pub/Sub nack/redelivery.
6. **Smallest reasonable fix:** perform an explicit null check and catch only `NumberFormatException`, matching the proposed SA-002 fix.
7. **Existing test coverage:** `GmailRealtimeServiceTest` covers valid, duplicate, failed-fetch, and stale-history synchronization paths, but not null/malformed/negative notification history IDs.
8. **Regression-test implication:** add service-level invalid-history tests before changing the private parser, including confirmation that sync progress is not advanced.

## Initial decision summary

| Decision | Findings | Action in this task |
|---|---|---|
| TRUE POSITIVE — likely real defect | None confirmed | None |
| CODE QUALITY / MAINTAINABILITY ISSUE | SA-002, SA-005, SA-006 | Recommend focused tests and small later refactors |
| FALSE POSITIVE | SA-001, SA-004 | Preserve code; consider justified narrow suppression only in a later approval task |
| NEEDS FURTHER INVESTIGATION | SA-003 | Decide CLI runtime-error/privacy policy before any code or suppression change |

No `BUG_LOG.md` entry was created. The report records static-analysis candidates and review judgements, not confirmed product defects.

## Recommended next step

Obtain human approval for the SA-003 CLI error-boundary policy. Then, in a separate test-first task, add regression tests for SA-002, SA-005, and SA-006 and implement only those approved maintainability fixes. Re-run SpotBugs without suppressions to measure the result; evaluate narrow documented suppressions for SA-001 and SA-004 only after their false-positive rationale is accepted.

---

## 2026-10-06 — Test-first remediation

Status: **COMPLETE — FOUR JUSTIFIED FINDINGS REMEDIATED, TWO DOCUMENTED FALSE POSITIVES REMAIN**

### Result summary

- Original unsuppressed findings: **6**.
- Post-fix unsuppressed findings: **2**.
- Findings removed: **SA-002, SA-003, SA-005, SA-006**.
- Findings remaining: **SA-001 and SA-004**, both retaining their original false-positive triage.
- Suppressions or exclusions added: **none**.
- Complete Java verification: **122 tests, 0 failures, 0 errors, 0 skipped**.

### SA-001 — `EI_EXPOSE_REP` in `ProfileCreateRequest`

- **Original finding:** the generated `interests()` record accessor was reported as exposing mutable internal state.
- **Triage decision:** false positive; the compact constructor stores the unmodifiable result of `Stream.toList()`, and `String` elements are immutable.
- **Test added:** none in this remediation task; the approved scope explicitly left this false positive for a later narrow-suppression decision.
- **Root cause:** SpotBugs does not prove the immutability established by the record constructor's `Stream.toList()` assignment.
- **Production change:** none.
- **Post-fix status:** remains in the unsuppressed report.
- **Rationale:** changing the accessor or adding a blanket exclusion would alter or obscure correct behavior solely to silence analysis.

### SA-002 — `DCN_NULLPOINTER_EXCEPTION` in `GmailApiClient.historyId`

- **Original finding:** the parser caught `NullPointerException` from `new BigInteger(value)` to identify a missing history ID.
- **Triage decision:** justified maintainability finding; preserve the existing `IllegalArgumentException` contract while removing NPE as expected control flow.
- **Tests added:** `parsesValidHistoryIdWithoutLosingPrecision` and `rejectsMissingMalformedAndNegativeHistoryIds` in `GmailApiClientTest`.
- **Root cause:** null and malformed text shared a multi-catch even though null can be checked directly.
- **Production change:** reject null explicitly before parsing and catch only `NumberFormatException`; valid, malformed, negative, and message behavior remain unchanged.
- **Post-fix status:** disappeared from the unsuppressed report.

### SA-003 — `REC_CATCH_EXCEPTION` in `GmailCommand.run`

- **Original finding:** one `catch (Exception)` sanitized every command failure, including unexpected programming/runtime failures.
- **Triage decision:** the privacy boundary remains required, but the approved policy confirmed that silently hiding unexpected runtime failures is a genuine diagnostic behavior defect.
- **Existing coverage:** `errorsAreReportedWithoutProviderDetails` already proved that an expected checked `IOException` produces a nonzero exit and sanitized user-visible output.
- **Test added:** `unexpectedRuntimeFailureIsDiagnosableWithoutLeakingSensitiveDetails` in `GmailCommandTest`.
- **Root cause:** expected OAuth/Gmail failures and unexpected unchecked failures used the same generic catch with no internal diagnostic distinction.
- **Production change:** catch the actual checked boundary types (`IOException` and `GeneralSecurityException`) separately; catch runtime failures at the outer privacy boundary, log only the safe command name and exception class, retain sanitized stderr, and set exit code 1. Exception messages, credentials, tokens, payloads, and mailbox content are not logged or printed.
- **Post-fix status:** disappeared from the unsuppressed report.
- **Behavior change:** expected operational failure behavior is unchanged; unexpected runtime failure behavior now includes safe internal diagnostic metadata and still cannot look successful.

### SA-004 — `EI_EXPOSE_REP2` in `GmailNotificationDecoder`

- **Original finding:** storing the constructor-injected `ObjectMapper` was reported as retaining a mutable external reference.
- **Triage decision:** false positive for this architecture; the mapper is trusted, application-managed Spring infrastructure.
- **Test added:** none; no configuration-isolation requirement was introduced.
- **Root cause:** SpotBugs cannot infer the trusted lifecycle of the injected Spring collaborator.
- **Production change:** none.
- **Post-fix status:** remains in the unsuppressed report.
- **Rationale:** copying the mapper could diverge from application-managed configuration, and blanket suppression was prohibited.

### SA-005 — `THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION` in `PubSubPullSubscriber.Handler`

- **Original finding:** `Handler.handle(byte[])` declared broad checked `Exception`.
- **Triage decision:** justified maintainability finding.
- **Test added:** `nacksCheckedIoFailure` in `PubSubPullSubscriberTest`; existing success and unchecked-failure tests were retained as regression coverage.
- **Root cause:** the functional interface was broader than its only checked caller requirement. `GmailHistorySyncService.sync` can propagate `IOException`; notification decoding and programming failures are unchecked.
- **Production change:** narrow the handler contract to `throws IOException`; dispatch now catches `IOException | RuntimeException`, preserving ack after success and nack after either failure category.
- **Post-fix status:** disappeared from the unsuppressed report.

### SA-006 — `DCN_NULLPOINTER_EXCEPTION` in `GmailHistorySyncService.historyId`

- **Original finding:** the synchronization parser used caught NPE to identify a null notification/stored history ID.
- **Triage decision:** justified maintainability finding with existing failure behavior preserved.
- **Test added:** `invalidNotificationHistoryIdsFailWithoutAdvancingProgress` in `GmailRealtimeServiceTest`; the existing valid synchronization test remains the valid-ID regression.
- **Root cause:** the private parser shared the same null-via-constructor-exception pattern as `GmailApiClient`.
- **Production change:** reject null explicitly and catch only `NumberFormatException`.
- **Post-fix status:** disappeared from the unsuppressed report.
- **Fallback/failure behavior:** null, malformed, and negative notification IDs still fail with `IllegalArgumentException`; stored progress remains unchanged and no Gmail call occurs.

### Verification record

- History parser pre-change characterization: **3 tests passed**.
- History parser post-change focused run: **3 tests passed**.
- History parser relevant regressions: **14 tests passed**.
- Pub/Sub pre-change characterization: **3 tests passed**.
- Pub/Sub post-change focused run: **1 test passed**.
- Pub/Sub relevant regressions: **3 tests passed**.
- Gmail command runtime-policy RED run: **1 test run, 1 expected failure** because no diagnostic metadata existed.
- Gmail command final focused run: **2 tests passed**.
- Gmail command relevant regressions: **7 tests passed**.
- Complete command: `.\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -Ddebug=false test`.
- Complete result: **BUILD SUCCESS — 122 tests run, 0 failures, 0 errors, 0 skipped in 27.292 seconds**.
- SpotBugs command: `.\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -DskipTests compile spotbugs:spotbugs`.
- SpotBugs result: **BUILD SUCCESS in 26.579 seconds; 2 unsuppressed findings across 50 analysed classes**.

No classifier, ranking, ingestion, database schema, profile scoring, Python, PMD, or JaCoCo change was made.

## Next recommended step

Review and approve narrow, documented per-finding suppressions for SA-001 and SA-004 only if the project requires a clean SpotBugs gate; otherwise retain the unsuppressed two-finding report as the accepted baseline.
