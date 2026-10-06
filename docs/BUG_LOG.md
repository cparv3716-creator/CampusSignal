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
