# Classifier Test Log

## 2026-10-06 — Discovery-stage test design

Stage: analysis/specification only. **No classifier tests have been implemented.**

The complete synthetic case-by-case matrix is in `docs/EMAIL_CLASSIFICATION_DISCOVERY.md`, section 11. It contains 32 per-category positive cases plus shared misleading-evidence, priority/conflict, and robustness cases. Every case records a test name, anonymised input, expected category, confidence class, rationale, and whether it derives from an observed mailbox pattern or an intentionally designed edge case.

### Proposed test groups and why they are necessary

| Group | Purpose | Mailbox basis |
|---|---|---|
| Clear positive category cases | Establish the minimum evidence for each category | Observed across the discovery/validation messages |
| Subject-only and body-only cases | Prevent dependence on one field | Sparse subjects and HTML/plain-text asymmetry were observed |
| Case, punctuation, boundary, and whitespace variants | Verify normalisation without substring overmatching | Mixed formatting and punctuation were observed; adversarial variants are extensions |
| Generic-keyword negatives | Prevent `opportunity`, `opening`, `research`, `event`, `apply`, or `register` from deciding alone | Observed across multiple categories |
| Signature/footer/URL/email negatives | Prevent non-content zones from creating evidence | Long signatures, social links, and mailing-list footers were observed |
| Reply/forward/quoted-chain cases | Separate the current authorial act from history and avoid duplicate evidence | Frequent observed pattern |
| Negation cases | Suppress explicitly negated category phrases | Intentionally designed extension; no reliable real negation example was found |
| Priority/conflict cases | Make overlapping functional purposes deterministic and reviewable | Academic events, career talks, calls, wellness events, and service notices overlapped |
| Tie/repetition cases | Avoid arbitrary enum order and keyword stuffing | Repeated quoted chains were observed; exact ties are an extension |
| Empty/null/HTML cases | Keep classification safe under malformed or sparse inputs | Sparse/HTML-heavy messages were observed; null cases are extensions |
| Recognition safeguards | Distinguish completed achievements from future prizes/applications | Missing-category failure observed in 6 validation messages |

### Observed-pattern cases

The following proposed tests are directly based on patterns in the sampled mailbox, but use synthetic text:

- academic seminar, quiz, course discussion, and workshop cases;
- internship, placement, graduate role, and career-session cases;
- event screenings, tutorials, exhibitions, and invitations;
- club-role applications, auditions, fellowship calls, and hackathon registration;
- circular, protocol, closure, outage, resumption, and facility-feedback notices;
- counselling access, assessment-period support, and institute wellness activity;
- auctions and merchandise falling to `GENERAL_OTHER`;
- completed award/achievement announcements;
- short reminders with quoted originals;
- forwarded wrappers, long signatures, and mailing-list footers;
- body-only evidence and substantive HTML with weak plain-text alternatives.

### Intentionally designed edge cases

- explicit negation (`not an internship`, `not an event`);
- keywords embedded only in a URL, address, signature, or footer;
- exact evidence ties;
- repeated identical category phrases;
- empty subject, empty body, and both empty;
- unusual whitespace, Unicode hyphens, punctuation, and mixed case;
- future award/prize calls that must not become recognition;
- unrelated and generic-only messages.

### Recommended first 20 JUnit cases

1. `classifiesSeminarFromSubjectAndBody`
2. `classifiesQuizFromBodyWhenSubjectIsGeneric`
3. `classifiesInternshipWithEmploymentContext`
4. `doesNotTreatGenericOpportunityAsCareer`
5. `classifiesScheduledScreeningAsEvent`
6. `classifiesAuditionAsParticipationCall`
7. `classifiesFacilityOutageAsAdministrativeNotice`
8. `classifiesCounsellingAccessAsWellbeingSupport`
9. `classifiesCompletedAwardAsRecognition`
10. `doesNotTreatFuturePrizeAsRecognition`
11. `fallsBackForMerchandiseOrder`
12. `ignoresKeywordInSignature`
13. `ignoresKeywordInMailingListFooter`
14. `ignoresKeywordInUrlAndEmailAddress`
15. `suppressesNegatedInternshipPhrase`
16. `prefersCurrentTextOverMisleadingQuotedHistory`
17. `usesQuotedAnnouncementAsMediumConfidenceRescue`
18. `doesNotBoostRepeatedQuotedPhrase`
19. `handlesHtmlConvertedTextAndUnicodePunctuation`
20. `returnsGeneralLowForEmptyOrTiedEvidence`

### Acceptance criteria for the future classifier

- All approved synthetic cases pass and contain no private mailbox text.
- Category output is one of the approved enum values; null/empty input is safe.
- Confidence is deterministic and follows the approved High/Medium/Low definition.
- Generic keywords alone never produce High confidence.
- Signature, footer, URL, email-address, and ignored quoted evidence cannot independently determine a category.
- Repeated copies of one phrase do not increase the score beyond one logical feature occurrence.
- Negated phrases do not count as positive evidence.
- `GENERAL_OTHER` remains reachable for no evidence, unresolved ties, and valid out-of-taxonomy messages.
- Career/academic/admin/wellbeing/call precedence over generic event form matches the approved specification.
- Recognition requires completed-achievement evidence and rejects future prize/application language.
- Focused classifier tests and the complete Maven/JUnit suite both pass.

### Implementation status

- Proposed test cases: documented.
- JUnit class/name: Pending human approval.
- Test implementation result: Not run; tests do not yet exist.
- Failures/fixes/regressions: None recorded for classifier code because classifier code was not changed.
- Full-suite result for this documentation stage: recorded in `docs/WORK_LOG.md` after verification.

## 2026-10-06 — Classification Specification V1 approval

Status: **PLANNED — NOT IMPLEMENTED**

The human-approved test source of truth is now section 14 of `docs/CLASSIFICATION_SPEC_V1.md`. The discovery matrix has been translated into planned V1 cases covering all eight approved categories, misleading evidence, primary-purpose conflicts, confidence mapping, quote handling, filtering, normalisation, negation, empty input, and attachment-only fallback.

### Approved expectation changes

- `wellness_activity_tie` is **PLANNED** with expected `EVENT_ACTIVITY`/`HIGH` for a normal public yoga/wellness gathering with date/venue and no explicit support-service context.
- `wellness_support_programme` is **PLANNED** with expected `WELLBEING_SUPPORT`/`HIGH` when the activity explicitly provides or forms part of an institute wellbeing support service.
- Non-selective academic registration is **PLANNED** as `ACADEMIC_RESEARCH`; selective academic/fellowship/project admission is **PLANNED** as `CALL_FOR_PARTICIPATION` unless employment makes it `CAREER`.
- Completed achievements are **PLANNED** as `RECOGNITION_ACHIEVEMENT`; future prizes, award applications, and competition calls are not.
- Every `GENERAL_OTHER` fallback is **PLANNED** with `LOW` confidence (0.5).

### First implementation batch — PLANNED

1. `classifiesSeminarFromSubjectAndBody` — PLANNED
2. `classifiesQuizFromBodyWhenSubjectIsGeneric` — PLANNED
3. `classifiesInternshipWithEmploymentContext` — PLANNED
4. `doesNotTreatGenericOpportunityAsCareer` — PLANNED
5. `classifiesScheduledScreeningAsEvent` — PLANNED
6. `classifiesNormalYogaGatheringAsEvent` — PLANNED
7. `classifiesExplicitWellbeingSupportProgrammeAsWellbeing` — PLANNED
8. `classifiesAuditionAsParticipationCall` — PLANNED
9. `classifiesFacilityOutageAsAdministrativeNotice` — PLANNED
10. `classifiesCounsellingAccessAsWellbeingSupport` — PLANNED
11. `classifiesCompletedAwardAsRecognition` — PLANNED
12. `doesNotTreatFuturePrizeAsRecognition` — PLANNED
13. `fallsBackForMerchandiseOrder` — PLANNED
14. `ignoresKeywordInSignatureFooterUrlAndAddress` — PLANNED
15. `suppressesSimpleNegatedCareerPhrase` — PLANNED
16. `prefersCurrentTextOverMisleadingQuotedHistory` — PLANNED
17. `usesQuotedAnnouncementAsMediumConfidenceRescue` — PLANNED
18. `doesNotBoostRepeatedLogicalEvidence` — PLANNED
19. `handlesHtmlUnicodePunctuationAndWhitespace` — PLANNED
20. `returnsGeneralLowForEmptyTiedOrAttachmentOnlyEvidence` — PLANNED

### Required V1 assertions — PLANNED

- one approved primary category only; no runner-up exposure;
- internal confidence class and persisted/API mapping (`HIGH` 0.9, `MEDIUM` 0.7, `LOW` 0.5);
- conceptual evidence weights and duplicate logical-evidence suppression;
- zero classification points from sender identity, signatures, footers, boilerplate, URLs, addresses, and social links;
- current-content priority, quote-only `MEDIUM` cap, and no repeated-quote accumulation;
- primary-purpose precedence and approved category boundaries;
- simple nearby negation suppression only;
- null/empty safety and `GENERAL_OTHER`/`LOW` attachment-only fallback;
- no mutation of stored email subject/body content.

### Current implementation status

- Classifier JUnit tests: **NOT STARTED**.
- Classifier production code: **NOT STARTED**.
- Failures/fixes/regression results: none; no classifier tests exist yet.
- Next step: implement the first classifier JUnit tests before production implementation.

## 2026-10-06 — Classifier Test Batch 1 RED → GREEN history

Test class: `src/test/java/edu/campussignal/RuleBasedEmailClassificationServiceTest.java`  
Tests created: **20 individual JUnit 5 tests**  
Current state: **PASS — classifier behavior implemented and verified**

Initial RED state: the tests compiled and executed against a minimal API/skeleton. `RuleBasedEmailClassificationService.classify` deliberately threw `UnsupportedOperationException`, so the first focused run reported 20 errors rather than assertion failures. This was the expected test-first RED state and was not an implementation bug. The final statuses below reflect the subsequently verified GREEN implementation.

| Test name | Specification behaviour | Origin | Expected category | Expected confidence | Status |
|---|---|---|---|---|---|
| `academicSubject` | Seminar subject corroborated by speaker, abstract, and date | Observed | `ACADEMIC_RESEARCH` | `HIGH` | PASS |
| `careerSubject` | Internship subject corroborated by employer, stipend, and eligibility | Observed | `CAREER` | `HIGH` | PASS |
| `eventSubject` | Screening with attendance context and logistics | Observed | `EVENT_ACTIVITY` | `HIGH` | PASS |
| `callSubject` | Non-employment coordinator application with eligibility/deadline | Observed | `CALL_FOR_PARTICIPATION` | `HIGH` | PASS |
| `adminSubject` | Mandatory laboratory-access circular | Observed | `ADMINISTRATIVE_NOTICE` | `HIGH` | PASS |
| `wellbeingSubject` | Counselling/mental-health access and service hours | Observed | `WELLBEING_SUPPORT` | `HIGH` | PASS |
| `recognitionSubject` | Completed award/win announcement | Validation | `RECOGNITION_ACHIEVEMENT` | `HIGH` | PASS |
| `generalSubject` | Clear out-of-specialist-scope hostel auction | Observed | `GENERAL_OTHER` | `HIGH` | PASS |
| `opportunityNotCareer` | Generic opportunity for attendance must not imply employment | Observed | `EVENT_ACTIVITY` | `HIGH` | PASS |
| `researchInJob` | Employment purpose overrides generic research wording | Observed | `CAREER` | `HIGH` | PASS |
| `prizeNotRecognition` | Future prize pool/registration is not completed recognition | Extension | `CALL_FOR_PARTICIPATION` | `HIGH` | PASS |
| `careerVsEvent` | Graduate-role purpose overrides event logistics | Observed | `CAREER` | `HIGH` | PASS |
| `academicVsEvent` | Scholarly seminar purpose overrides ordinary event structure | Observed | `ACADEMIC_RESEARCH` | `HIGH` | PASS |
| `callVsEvent` | Audition/selection purpose overrides ordinary attendance | Observed | `CALL_FOR_PARTICIPATION` | `HIGH` | PASS |
| `quoteMisleading` | Specific current service-resumption text overrides conflicting quoted internship | Extension | `ADMINISTRATIVE_NOTICE` | `HIGH` | PASS |
| `quoteOnlyRescue` | Insufficient current reminder may use a complete quoted job announcement | Observed | `CAREER` | `MEDIUM` | PASS |
| `emptyBoth` | Null subject/body falls back safely | Extension | `GENERAL_OTHER` | `LOW` | PASS |
| `emptySubject` | Body-only counselling evidence classifies without a subject | Extension | `WELLBEING_SUPPORT` | `MEDIUM` | PASS |
| `emptyBody` | Strong internship subject with no body is capped below High | Extension | `CAREER` | `MEDIUM` | PASS |
| `unicodeHyphen` | Unicode hyphen variants normalise for career evidence | Extension | `CAREER` | `HIGH` | PASS |

### Verification

- Focused command: `.\\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" "-Dtest=RuleBasedEmailClassificationServiceTest" -Ddebug=false test`
- Initial RED result: **BUILD FAILURE — 20 tests run, 0 failures, 20 errors, 0 skipped** because every test reached the intentional `UnsupportedOperationException` skeleton.
- First GREEN run after implementing the classifier: **BUILD SUCCESS — 20 tests run, 0 failures, 0 errors, 0 skipped**.
- Final focused rerun after correcting reply/forward-prefix preprocessing: **BUILD SUCCESS — 20 tests run, 0 failures, 0 errors, 0 skipped** in 14.136 seconds.
- Existing-suite command: `.\\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" "-Dtest=!RuleBasedEmailClassificationServiceTest" -Ddebug=false test`
- Existing-suite result: **BUILD SUCCESS — 77 tests run, 0 failures, 0 errors, 0 skipped**.
- Final complete-suite command: `.\\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -Ddebug=false test`
- Final complete-suite result: **BUILD SUCCESS — 97 tests run, 0 failures, 0 errors, 0 skipped** in 26.120 seconds.

### Implementation status

- Category/confidence/result/service compile contract: created.
- Rule-based classifier business logic: **IMPLEMENTED for approved Batch 1**.
- Batch-1 tests: **20/20 PASS**.
- Next step: add the next approved test-first robustness batch for filtering, negation, ties, academic registration, facility surveys, wellbeing/event boundaries, HTML, and dedicated reply/forward-prefix handling before broadening the implementation.

## 2026-10-06 — Classifier Robustness Test Batch 2 initial run

Test class: `src/test/java/edu/campussignal/RuleBasedEmailClassificationServiceTest.java`
New tests: **20 individual JUnit 5 tests**
Total classifier tests: **40**
Production classifier during run: **unchanged Batch-1 implementation**

| Test name | Specification behaviour | Origin | Expected result | Initial status | Final status |
|---|---|---|---|---|---|
| `replyPrefixNormalization` | Ignore `Re:` while retaining academic subject evidence | Observed | `ACADEMIC_RESEARCH` / `HIGH` | PASS | PASS |
| `forwardPrefixNormalization` | Ignore `Fwd:` while retaining employment subject evidence | Observed | `CAREER` / `HIGH` | PASS | PASS |
| `signatureKeywordIgnored` | Ignore misleading career wording in a signature | Observed | `ADMINISTRATIVE_NOTICE` / `HIGH` | PASS | PASS |
| `footerKeywordIgnored` | Ignore research/event/opportunity wording in a mailing-list footer | Observed | `ADMINISTRATIVE_NOTICE` / `HIGH` | PASS | PASS |
| `urlKeywordIgnored` | Ignore a career-looking URL while using facility-service feedback evidence | Extension | `ADMINISTRATIVE_NOTICE` / `MEDIUM` | FAIL | PASS |
| `emailAddressKeywordIgnored` | Ignore research wording inside an email address | Extension | `ADMINISTRATIVE_NOTICE` / `HIGH` | PASS | PASS |
| `negatedInternship` | Suppress negated job/internship hits and retain volunteer application purpose | Extension | `CALL_FOR_PARTICIPATION` / `MEDIUM` | FAIL | PASS |
| `negatedEvent` | Suppress negated event wording for an asynchronous academic resource | Extension | `ACADEMIC_RESEARCH` / `MEDIUM` | FAIL | PASS |
| `futureAwardNotRecognition` | Treat a future award application as a participation call | Validation | `CALL_FOR_PARTICIPATION` / `HIGH` | PASS | PASS |
| `exactTieFallsBackToGeneral` | Return fallback for equally supported co-primary academic/event purposes | Extension | `GENERAL_OTHER` / `LOW` | FAIL | PASS |
| `repeatedQuotedEvidenceDoesNotBoost` | Prevent repeated quoted internship wording from overriding a current event | Observed | `EVENT_ACTIVITY` / `MEDIUM` | PASS | PASS |
| `repeatedCurrentKeywordDoesNotArtificiallyBoost` | Prevent repeated generic opportunity wording from manufacturing evidence | Extension | `GENERAL_OTHER` / `LOW` | PASS | PASS |
| `htmlBodyClassification` | Classify visible academic text inside simple HTML | Observed | `ACADEMIC_RESEARCH` / `HIGH` | PASS | PASS |
| `htmlEntitiesDecoded` | Preserve useful academic meaning around HTML entities | Extension | `ACADEMIC_RESEARCH` / `HIGH` | PASS | PASS |
| `facilityFeedbackIsAdministrative` | Treat institute dining-service operation feedback as administrative | Validation | `ADMINISTRATIVE_NOTICE` | FAIL | PASS |
| `genericSurveyIsNotAdministrative` | Keep a generic club/opinion survey out of administration | Extension | `GENERAL_OTHER` / `LOW` | PASS | PASS |
| `nonSelectiveAcademicRegistration` | Keep non-selective course/workshop registration academic | Validation | `ACADEMIC_RESEARCH` / `MEDIUM` | PASS | PASS |
| `selectiveAcademicApplication` | Treat limited-seat academic selection as a participation call | Extension | `CALL_FOR_PARTICIPATION` / `HIGH` | PASS | PASS |
| `publicYogaActivityIsEvent` | Treat an ordinary public yoga activity without support access as an event | Observed | `EVENT_ACTIVITY` / `MEDIUM` | FAIL | PASS |
| `wellbeingServiceEventIsWellbeing` | Prefer explicit counselling/support-service purpose over event structure | Observed | `WELLBEING_SUPPORT` / `HIGH` | PASS | PASS |

### Initial Batch-2 verification

- Focused command: `.\\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" "-Dtest=RuleBasedEmailClassificationServiceTest" -Ddebug=false test`
- Combined result: **BUILD FAILURE — 40 tests run, 6 failures, 0 errors, 0 skipped**.
- Batch-2 result: **14 PASS, 6 FAIL, 0 ERROR**.
- Original Batch-1 isolation result: **BUILD SUCCESS — 20 tests run, 0 failures, 0 errors, 0 skipped**.
- No production classifier change was made before or after this run.

### Initial failures

| Test | Expected | Actual | Likely reason |
|---|---|---|---|
| `urlKeywordIgnored` | `ADMINISTRATIVE_NOTICE` / `MEDIUM` | `GENERAL_OTHER` / `LOW` | URL masking works, but facility/service feedback is only contextual and does not yet qualify as administrative evidence. |
| `negatedInternship` | `CALL_FOR_PARTICIPATION` / `MEDIUM` | `CAREER` / `MEDIUM` | V1 nearby-negation suppression is not implemented, so negated `internship`/`job` tokens still activate career evidence. |
| `negatedEvent` | `ACADEMIC_RESEARCH` / `MEDIUM` | `ACADEMIC_RESEARCH` / `HIGH` | Category is correct, but the negated event phrase is not suppressed/resolved as limited ambiguity, so confidence remains High. |
| `exactTieFallsBackToGeneral` | `GENERAL_OTHER` / `LOW` | `ACADEMIC_RESEARCH` / `HIGH` | The current classifier returns the first qualifying purpose gate and has no explicit equal-evidence tie detection. |
| `facilityFeedbackIsAdministrative` | `ADMINISTRATIVE_NOTICE` | `GENERAL_OTHER` / `LOW` | Institute-service feedback/timing/quality language is not yet a qualifying administrative rule. |
| `publicYogaActivityIsEvent` | `EVENT_ACTIVITY` / `MEDIUM` | `WELLBEING_SUPPORT` / `HIGH` | The current classifier scores negated counselling/support-service wording as positive wellbeing evidence. |

At the test-creation stage these six failures were treated as test-first specification gaps rather than six fabricated bugs. Subsequent implementation confirmed three shared root-cause defects: `CS-CLS-002`, `CS-CLS-003`, and `CS-CLS-004`.

### Batch-2 GREEN verification

- Facility-feedback group: **3 tests run, 0 failures, 0 errors** (`facilityFeedbackIsAdministrative`, `urlKeywordIgnored`, `genericSurveyIsNotAdministrative`).
- Negation plus career/event regressions: **6 tests run, 0 failures, 0 errors**.
- Equal-purpose tie plus precedence regressions: **4 tests run, 0 failures, 0 errors**.
- Wellbeing/event boundary plus wellbeing regressions: **4 tests run, 0 failures, 0 errors**.
- Final classifier result: **BUILD SUCCESS — 40 tests run, 0 failures, 0 errors, 0 skipped** in 7.277 seconds.
- Final complete Java result: **BUILD SUCCESS — 117 tests run, 0 failures, 0 errors, 0 skipped** in 26.634 seconds.
- No Batch-1 or Batch-2 test expectation was changed, weakened, disabled, or skipped.

---

## 2026-10-07 — Final classifier coverage tests

New JUnit tests: **2**. Baseline: 43 classifier tests and 135 total Java tests. Final: **45 classifier tests and 137 total Java tests**.

| Test | Behavioral purpose | First-run result |
|---|---|---|
| `subjectNegationSuppressesCareerCueWithoutHidingAcademicPurpose` | Verifies that a subject-side `internship` cue inside the established four-token negation window is suppressed, while valid current academic evidence still selects `ACADEMIC_RESEARCH` with V1 `MEDIUM` confidence. | PASS — isolated 1/1 |
| `namedActivityWithSubjectScheduleIsAnEvent` | Verifies the existing composite subject event rule: attendance language, a named activity, and concrete schedule/location in a subject-only message select `EVENT_ACTIVITY` with V1 `MEDIUM` confidence. | PASS — isolated 1/1 |

### Ordered verification

- Subject-negation isolated command: `.\mvnw.cmd -B -ntp "-Dtest=RuleBasedEmailClassificationServiceTest#subjectNegationSuppressesCareerCueWithoutHidingAcademicPurpose" test` — BUILD SUCCESS; 1 test, 0 failures, 0 errors, 0 skipped.
- Classifier run after subject-negation verification: `.\mvnw.cmd -B -ntp "-Dtest=RuleBasedEmailClassificationServiceTest" test` — BUILD SUCCESS; 45 tests, 0 failures, 0 errors, 0 skipped.
- Composite-event isolated command: `.\mvnw.cmd -B -ntp "-Dtest=RuleBasedEmailClassificationServiceTest#namedActivityWithSubjectScheduleIsAnEvent" test` — BUILD SUCCESS; 1 test, 0 failures, 0 errors, 0 skipped.
- Classifier run after composite-event verification: `.\mvnw.cmd -B -ntp "-Dtest=RuleBasedEmailClassificationServiceTest" test` — BUILD SUCCESS; 45 tests, 0 failures, 0 errors, 0 skipped.
- Non-destructive 135-test baseline reproduction excluded exactly the two new methods and passed with 43 classifier tests / 135 total tests, 0 failures, 0 errors, and 0 skipped.
- Final command: `.\mvnw.cmd -B -ntp clean test` — BUILD SUCCESS; **137 tests, 0 failures, 0 errors, 0 skipped** in 49.145 seconds.

All original 43 classifier tests remained unchanged, enabled, and passing. Both new tests passed immediately, so no production defect or production-code change was required.

---

## 2026-10-06 — Test-first SpotBugs remediation

New JUnit tests: **5**

| Test | Class | Behavior protected | Final status |
|---|---|---|---|
| `parsesValidHistoryIdWithoutLosingPrecision` | `GmailApiClientTest` | A valid unsigned Gmail history ID is parsed as an arbitrary-precision integer. | PASS |
| `rejectsMissingMalformedAndNegativeHistoryIds` | `GmailApiClientTest` | Null/missing, malformed, and negative IDs retain their established `IllegalArgumentException` messages. | PASS |
| `invalidNotificationHistoryIdsFailWithoutAdvancingProgress` | `GmailRealtimeServiceTest` | Invalid notification IDs fail before Gmail access and do not advance stored synchronization progress. | PASS |
| `nacksCheckedIoFailure` | `PubSubPullSubscriberTest` | A checked `IOException` from notification handling nacks and never acks the Pub/Sub message. | PASS |
| `unexpectedRuntimeFailureIsDiagnosableWithoutLeakingSensitiveDetails` | `GmailCommandTest` | An unchecked failure exits nonzero, emits sanitized user output, records safe diagnostic metadata, leaks no exception message, and prints no success summary. | PASS |

### Ordered verification

- History characterization before production change: `.\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" "-Dtest=GmailApiClientTest#parsesValidHistoryIdWithoutLosingPrecision+rejectsMissingMalformedAndNegativeHistoryIds,GmailRealtimeServiceTest#invalidNotificationHistoryIdsFailWithoutAdvancingProgress" -Ddebug=false test` — **3 passed**.
- History focused run after production change: same command — **3 passed**.
- History regressions: `.\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" "-Dtest=GmailApiClientTest,GmailRealtimeServiceTest" -Ddebug=false test` — **14 passed**.
- Pub/Sub characterization before signature change: `.\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" "-Dtest=PubSubPullSubscriberTest" -Ddebug=false test` — **3 passed**.
- Pub/Sub focused run after signature change: `PubSubPullSubscriberTest#nacksCheckedIoFailure` — **1 passed**.
- Pub/Sub regressions after signature change: `PubSubPullSubscriberTest` — **3 passed**.
- Gmail command initial policy run: `GmailCommandTest#unexpectedRuntimeFailureIsDiagnosableWithoutLeakingSensitiveDetails` — **RED as intended: 1 test run, 1 failure**, because the original broad catch emitted no safe diagnostic metadata.
- The first post-change run exposed that Spring output capture did not retain the Logback event even though it appeared in the console. The test was corrected to inspect a temporary logger appender; production behavior was not changed for this harness issue.
- Gmail command final focused run: `GmailCommandTest#errorsAreReportedWithoutProviderDetails+unexpectedRuntimeFailureIsDiagnosableWithoutLeakingSensitiveDetails` — **2 passed**.
- Gmail command regressions: `GmailCommandTest` — **7 passed**.

### Complete verification

- Command: `.\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -Ddebug=false test`.
- Result: **BUILD SUCCESS — 122 tests run, 0 failures, 0 errors, 0 skipped in 27.292 seconds**.
- Baseline comparison: 117 tests before remediation; 122 after adding five focused tests.
- SpotBugs command: `.\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2" -DskipTests compile spotbugs:spotbugs`.
- SpotBugs result: **BUILD SUCCESS; findings reduced from 6 to 2**, with only the two documented false positives remaining.

---

## 2026-10-06 — PMD remediation characterization and regression tests

New JUnit tests: **10**. Baseline: 122 tests. Final total: **132 tests**.

| Test | Class | Behavior protected | Final status |
|---|---|---|---|
| `ignoresMissingBlankAndDuplicateIdsAcrossPages` | `GmailApiClientTest` | Missing/blank provider IDs are ignored, duplicate IDs retain first-seen order, the remaining maximum is sent on page two, and no page beyond the result limit is fetched. | PASS |
| `blankNextPageTokenEndsPagination` | `GmailApiClientTest` | A blank provider next-page token terminates pagination without another API call. | PASS |
| `rejectsBlankJsonDocument` | `GmailNotificationDecoderTest` | Empty decoded notification bytes retain the sanitized non-object failure. | PASS |
| `rejectsMissingRequiredFieldsWithTheirExistingMessages` | `GmailNotificationDecoderTest` | Missing email/history fields retain their established exception type and messages. | PASS |
| `rejectsNonNumericTextualHistoryId` | `GmailNotificationDecoderTest` | Non-digit textual history IDs remain rejected. | PASS |
| `emptyHistoryAdvancesProgressWithoutFetchingMessages` | `GmailRealtimeServiceTest` | An empty history page advances its usable history ID and performs no message fetch. | PASS |
| `ignoresMissingNestedMessagesAndDeduplicatesIdsAcrossPages` | `GmailRealtimeServiceTest` | Missing messages-added data, null messages, null/blank IDs, and duplicate IDs across pages are harmless and deterministic. | PASS |
| `repeatedPrefixesAndUnicodeSpacingNormalizeTogether` | `RuleBasedEmailClassificationServiceTest` | Repeated `Re:`/`Fwd:` prefixes plus Unicode spacing/punctuation preserve classification. | PASS |
| `forwardedMessageBoundaryKeepsQuotedEvidenceOutOfCurrentMessage` | `RuleBasedEmailClassificationServiceTest` | Forwarded-message boundaries prevent quoted career evidence from overriding the current administrative purpose. | PASS |
| `nearbyNegationWindowIncludesExactlyFourPrecedingTokens` | `RuleBasedEmailClassificationServiceTest` | The existing negation window includes exactly four preceding normalized tokens and excludes the fifth. | PASS |

### Ordered verification

- Gmail API characterization before refactor: `GmailApiClientTest` — **11 passed**.
- Gmail API after final refactor: `GmailApiClientTest` — **11 passed**; complete suite — **124 passed**; PMD — **30 findings**.
- Notification characterization before refactor: `GmailNotificationDecoderTest` — **5 passed**.
- Notification after refactor: `GmailNotificationDecoderTest` — **5 passed**; complete suite — **127 passed**; PMD — **29 findings**.
- History characterization initially had one test-source parenthesis compilation error; it was corrected before production code changed. Characterization rerun — **7 passed**.
- History after refactor: `GmailRealtimeServiceTest` — **7 passed**; complete suite — **129 passed**; PMD — **28 findings**.
- Classifier characterization before extraction: `RuleBasedEmailClassificationServiceTest` — **43 passed**.
- Classifier after final extraction: **43 passed**, including all unchanged original 40; complete suite — **132 passed**; PMD — **27 findings**.

### Complete result

- Command: `.\mvnw.cmd -B -ntp clean test`.
- Final result: **BUILD SUCCESS — 132 tests run, 0 failures, 0 errors, 0 skipped in 37.600 seconds**.
- No expectation was changed, weakened, disabled, or skipped.

---

## 2026-10-06 — Gmail history safety coverage tests

New JUnit tests: **3**. Baseline: 132 tests. Final total: **135 tests**.

| Test | Behavioral purpose | First-run result |
|---|---|---|
| `repeatedHistoryPageTokenFailsWithoutAdvancingProgress` | Prevent infinite history pagination: a repeated nonblank token fails safely after two page calls, fetches no messages, and preserves stored progress. | PASS |
| `partialStaleHistoryRecoveryFailureDoesNotAdvanceProgress` | Protect incomplete bounded recovery: successful partial persistence remains retry-safe, the failure propagates, stored progress is unchanged, and no current profile history ID is read as a false success marker. | PASS |
| `duplicateMessageWithoutPageHistoryIdUsesNotificationProgress` | Protect idempotent history replay and documented fallback: an existing message is counted but not duplicated, and absent page history ID falls back to the notification history ID. | PASS |

### Test-first execution

- After test 1: `.\mvnw.cmd -B -ntp "-Dtest=GmailRealtimeServiceTest" test` — BUILD SUCCESS; **8 tests, 0 failures, 0 errors, 0 skipped**.
- After test 2: same command — BUILD SUCCESS; **9 tests, 0 failures, 0 errors, 0 skipped**.
- After test 3/final focused run: same command — BUILD SUCCESS; **10 tests, 0 failures, 0 errors, 0 skipped**.
- Every new test passed immediately against the existing implementation. No production defect was exposed, so no production code or `BUG_LOG.md` entry was needed.

### Complete verification

- Command: `.\mvnw.cmd -B -ntp clean test`.
- Result: **BUILD SUCCESS — 135 tests run, 0 failures, 0 errors, 0 skipped in 47.641 seconds**.
- No existing test was weakened, modified, disabled, or skipped.

---

