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

