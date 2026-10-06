# Bug Log

## 2026-10-06 — Discovery-only stage

No implementation bugs were recorded during this discovery-only analysis/specification stage. The classifier was not implemented or modified, so validation findings are taxonomy/specification risks rather than software defects.

The six validation messages that exposed a missing recognition/achievement type are documented in `docs/EMAIL_CLASSIFICATION_DISCOVERY.md` as a validation-driven specification change, not as fabricated implementation bugs.

---

## CS-CLS-001 — Reply/forward prefix removed too late

- Date: 2026-10-06
- Stage: Classifier Test Batch 1 — GREEN phase
- Symptom: `Re:` and `Fwd:` punctuation was normalised before the prefix-removal expression ran, leaving a leading `re` or `fwd` token in classification evidence.
- Minimal reproduction: normalising subject `Re: Pool status` produced `re pool status` instead of `pool status`.
- Expected behaviour: remove leading reply/forward prefixes before general punctuation normalisation.
- Actual behaviour: the colon was converted to whitespace first, so the matcher requiring `:` no longer matched.
- Root cause: preprocessing operations were applied in the wrong order in `normalizeSubject`.
- Fix: perform Unicode/case normalisation, remove reply/forward prefixes while their delimiters remain, and then apply the general evidence normaliser.
- Status: Fixed before final GREEN verification.
- Regression status: the focused 20-test classifier suite (including two `Re:` scenarios) and the complete 97-test suite are rerun after this fix; a dedicated prefix-only assertion remains appropriate for a later robustness batch.

---

## CS-CLS-002 — Institute facility/service feedback not recognised

- Date: 2026-10-06
- Stage: Classifier Test Batch 2 — GREEN phase
- Symptom: operational feedback about an institute facility/service fell back to `GENERAL_OTHER`/`LOW`.
- Failing tests: `facilityFeedbackIsAdministrative`, `urlKeywordIgnored`.
- Expected behaviour: specific feedback concerning facility/service operation, timings, or quality is `ADMINISTRATIVE_NOTICE`; generic surveys remain eligible for `GENERAL_OTHER`.
- Actual behaviour: the existing rule treated facility/service/feedback wording only as context, so no administrative category qualified.
- Root cause: `administrativeEvidence` had policy, closure, outage, and resumption indicators but no qualifying institute-service feedback combination.
- Fix: added a conjunctive facility-feedback rule requiring a feedback request, an identified institute facility/service, and operational context; it yields Medium evidence without making every survey administrative.
- URL-filter finding: URL masking already worked. The career-looking URL was removed before matching, so this was not a URL-masking defect.
- Focused verification: facility-feedback group passed 3/3, including `genericSurveyIsNotAdministrative` as the negative control.
- Regression verification: 40 classifier tests and all 117 Java tests passed.
- Status: Fixed.

---

## CS-CLS-003 — Nearby negated category cues counted as positive evidence

- Date: 2026-10-06
- Stage: Classifier Test Batch 2 — GREEN phase
- Symptom: obvious phrases such as `not an internship`, `not a job`, `not an event`, and `no counselling or support-service access` still contributed positive category evidence; resolved negation could also leave confidence at `HIGH`.
- Failing tests: `negatedInternship`, `negatedEvent`, `publicYogaActivityIsEvent`.
- Expected behaviour: suppress the nearby negated category hit, evaluate remaining evidence normally, and cap a supported result involving explicit resolved negation at `MEDIUM`.
- Actual behaviour: career/wellbeing evidence matched the negated words, and academic confidence remained `HIGH` despite the explicit negated competing cue.
- Root cause: phrase matching had no bounded negation check and confidence calculation did not know that a conflict had been resolved through negation.
- Fix: added a small four-token nearby-negation window for approved category cues, unnegated-evidence matching for affected categories, recognition of `applications are open`, and a `MEDIUM` confidence cap when classification resolves an explicit negated cue.
- Wellbeing finding: generic `wellness`/`yoga` alone was already insufficient. The yoga false positive came from negated counselling/support wording, so no separate wellbeing bug ID was created.
- Focused verification: negation/career/event group passed 6/6; wellbeing/event group passed 4/4.
- Regression verification: 40 classifier tests and all 117 Java tests passed.
- Status: Fixed.

---

## CS-CLS-004 — Equal co-primary evidence resolved by rule order

- Date: 2026-10-06
- Stage: Classifier Test Batch 2 — GREEN phase
- Symptom: a message with equally strong independent academic-seminar and event-screening purposes returned `ACADEMIC_RESEARCH`/`HIGH`.
- Failing test: `exactTieFallsBackToGeneral`.
- Expected behaviour: a genuine equal-evidence conflict with no single primary purpose returns `GENERAL_OTHER`/`LOW`; ordinary event structure must still lose to approved career, academic, or selection purpose rules.
- Actual behaviour: `classifyCurrent` returned the first qualifying category, so academic rule order silently broke the tie.
- Root cause: evidence was evaluated through early category returns without an explicit equal independent-purpose check.
- Fix: detect an exact score tie when both academic and event purposes have independent strong subject and body evidence, and fall back before applying the ordinary academic-over-event rule.
- Focused verification: tie plus precedence group passed 4/4 (`exactTieFallsBackToGeneral`, `careerVsEvent`, `academicVsEvent`, `callVsEvent`).
- Regression verification: 40 classifier tests and all 117 Java tests passed.
- Status: Fixed.
