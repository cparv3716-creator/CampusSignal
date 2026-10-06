# CampusSignal Classification Specification V1

Status: **Human-approved specification**  
Approved: 2026-10-06  
Implementation status: **IN PROGRESS — Batch 1 GREEN; remaining V1 cases are not yet implemented**  
Evidence source: `docs/EMAIL_CLASSIFICATION_DISCOVERY.md`

This document is the source of truth for the first deterministic Java email classifier. If the earlier discovery document conflicts with this specification, this document controls V1 behavior.

## 1. Scope

V1 classifies an email's primary functional purpose using deterministic evidence from the subject and body. It produces exactly one primary category and one internal confidence class.

V1 does not:

- expose runner-up categories;
- classify attachment contents;
- use machine learning, LLMs, embeddings, semantic ranking, sender allow-lists, or behavioural data;
- modify or delete original stored email content;
- treat its confidence values as calibrated probabilities.

## 2. Approved categories

The only V1 category values are:

1. `ACADEMIC_RESEARCH`
2. `CAREER`
3. `EVENT_ACTIVITY`
4. `CALL_FOR_PARTICIPATION`
5. `ADMINISTRATIVE_NOTICE`
6. `WELLBEING_SUPPORT`
7. `RECOGNITION_ACHIEVEMENT`
8. `GENERAL_OTHER`

Every email receives one category. A genuine unresolved tie, empty/no usable evidence, generic-only evidence, or attachment-only meaning returns `GENERAL_OTHER` with `LOW` confidence.

## 3. Classification target

Classification is based on the message's **primary functional purpose**, not every topic or keyword it contains. The precedence rules in section 8 resolve common purpose overlaps. They are not a blind global enum ordering.

Examples:

- A recruitment talk with a graduate-role application is `CAREER`, although it also has event logistics.
- A research seminar is `ACADEMIC_RESEARCH`, although it has a date and venue.
- A stadium-closure notice is `ADMINISTRATIVE_NOTICE`, although an event caused the closure.
- A public yoga class is normally `EVENT_ACTIVITY`; it becomes `WELLBEING_SUPPORT` only when it explicitly provides or forms part of an institute support service.

## 4. Input and evidence zones

The classifier may receive a nullable/blank subject and body. Evidence must be separated, where reasonably identifiable, into:

- normalised subject;
- current authored body content;
- quoted previous content;
- signature;
- mailing-list footer/boilerplate;
- URLs, email addresses, and social links.

Only the subject, current authored body, and the limited quote rescue described in section 10 can earn classification points.

The following receive zero evidence points:

- signatures;
- mailing-list footers;
- generic boilerplate;
- URLs;
- email addresses;
- social links;
- sender identity by itself.

Sender identity may be retained as non-scoring corroborative/audit information, but it must not determine a category or add points in V1.

## 5. Normalisation

Evidence matching must be:

- case-insensitive;
- Unicode-aware for whitespace;
- tolerant of common Unicode hyphen and punctuation variants;
- based on words and phrases with boundaries;
- tolerant of normal `Re:` and `Fwd:` prefixes for evidence matching;
- protected by duplicate logical-evidence suppression.

V1 must not use arbitrary substring matching. Normalisation is performed only on a classification working copy; the original stored subject/body remains unchanged.

For multipart/HTML messages, use substantive cleaned plain text when available. If the plain-text alternative is empty, footer-only, or otherwise non-substantive, use safely converted visible HTML text.

## 6. Evidence scoring

The approved conceptual weights are:

| Evidence | Points |
|---|---:|
| Strong subject evidence | 3 |
| Strong current-body evidence | 2 |
| Medium/contextual evidence | 1 |
| Signature/footer/URL/email-address evidence | 0 |
| Sender identity | 0 |

Rules:

- Count the same logical evidence only once per message/category.
- Repetition of the same keyword or a repeated quoted chain does not increase the score.
- A generic word such as `opportunity`, `opening`, `event`, `research`, `session`, `apply`, `register`, `award`, `winner`, or `prize` is not strong evidence by itself.
- Medium/contextual evidence supports category-specific evidence but generic medium evidence alone must not force a specialist category.
- A category needs usable, category-specific evidence; otherwise the result is `GENERAL_OTHER`/`LOW`.

## 7. Confidence

Internal confidence classes and persisted/API mappings are:

| Internal class | Persisted/API value |
|---|---:|
| `HIGH` | 0.9 |
| `MEDIUM` | 0.7 |
| `LOW` | 0.5 |

These values are deterministic confidence levels, **not calibrated probabilities** and not estimates that 90%, 70%, or 50% of comparable messages are correct.

### `HIGH`

Requires either:

- strong, corroborated current-message evidence across subject and current body; or
- multiple independent strong indicators in the current message.

Quoted evidence cannot make a result `HIGH`.

### `MEDIUM`

Applies when the category is supported but lacks current-message corroboration, including:

- a strong subject with an otherwise empty/non-substantive body;
- one strong current-body indicator with compatible context;
- quote-only rescue after insufficient current content;
- a supported result with limited, non-genuine ambiguity resolved by the approved primary-purpose rules.

### `LOW`

Required for:

- `GENERAL_OTHER` fallback;
- unresolved genuine conflict/tie;
- generic-only evidence;
- empty or unusable subject/body;
- attachment-only meaning;
- insufficient evidence for a specialist category.

## 8. Primary-purpose precedence

Apply these purpose rules only when the described specific evidence exists:

1. Explicit employment, internship, placement, job, or graduate-role purpose produces `CAREER` over generic application/event evidence.
2. Closure, outage, resumption, policy, mandatory instruction, or service operation produces `ADMINISTRATIVE_NOTICE` over generic event language.
3. Course, assessment, seminar, scholarly lecture, or research workshop produces `ACADEMIC_RESEARCH` over ordinary event structure.
4. Counselling, mental-health support, stress-support access, wellness-centre access, appointments, or explicit institute support-service access produces `WELLBEING_SUPPORT`.
5. Selection, submission, audition, or non-employment role/programme application produces `CALL_FOR_PARTICIPATION` over ordinary `EVENT_ACTIVITY`.
6. A completed achievement, award, win, honour, or completed-result announcement whose primary purpose is recognition produces `RECOGNITION_ACHIEVEMENT`.
7. A genuine unresolved tie produces `GENERAL_OTHER`/`LOW`.

## 9. Category rules

### `ACADEMIC_RESEARCH`

Include courses, assessments, seminars, scholarly lectures, research workshops, academic resources, and non-selective academic registration.

Strong evidence includes explicit seminar/course/quiz/assessment language with academic details such as speaker, abstract, course context, assessment slots, or instructional content.

Exclude employment, completed achievements, ordinary attendance events, and selective/competitive academic applications.

Boundary rule:

- Non-selective registration for a course, academic workshop, or learning programme is `ACADEMIC_RESEARCH`.
- A selective application with limited seats, eligibility, admission/selection criteria, fellowship/project selection, or competitive evaluation is `CALL_FOR_PARTICIPATION`, unless employment makes it `CAREER`.

### `CAREER`

Include jobs, internships, placements, hiring, graduate roles, pre-placement activity tied to hiring, and explicit career preparation.

Strong evidence includes an employment role/employer plus one or more of eligibility, compensation/stipend, resume/application process, interview process, or joining timeline.

Exclude club positions, non-employment fellowships/projects, ordinary events, and competitions. `Opportunity`, `opening`, `apply`, a company name, or a salary-like number alone is insufficient.

### `EVENT_ACTIVITY`

Include scheduled gatherings whose primary purpose is attendance: screenings, exhibitions, cultural/religious gatherings, public club activities, talks that are not primarily academic/career, and normal yoga/meditation/wellness activities.

Strong evidence includes a named activity with attendance language and concrete date/time/venue or online mode.

Exclude assessments, employment, selective calls, service operations, completed recognition, and explicit wellbeing support services.

### `CALL_FOR_PARTICIPATION`

Include non-employment calls to apply, audition, submit, join a team, take a club/project role, enter a contest/hackathon, or seek selection to a limited programme.

Strong evidence includes a selection/application action plus eligibility, deliverable, audition, selection process, limited places, or deadline.

Exclude employment, ordinary event RSVP/registration, merchandise orders, facility surveys, and non-selective academic registration.

### `ADMINISTRATIVE_NOTICE`

Include policies, circulars, mandatory instructions, closures, outages, resumptions, operating changes, official schedules, governance notices, and institute service/facility feedback.

Facility-feedback examples that may qualify:

- dining-facility feedback;
- hostel internet/service feedback;
- library timing/service surveys.

Generic club surveys, research questionnaires, and unrelated opinion polls are not automatically administrative; they require another clearly supported category or fall back to `GENERAL_OTHER`.

Exclude event schedules, assessment schedules, recruitment/application updates, and official-sender-only evidence.

### `WELLBEING_SUPPORT`

Keep this category narrow. Include:

- counselling and mental-health services;
- stress-support access;
- wellness-centre access;
- appointments, walk-ins, helplines, or professional support availability;
- explicit institute wellbeing support programmes.

A normal public yoga, meditation, fitness, or wellness gathering whose purpose is attendance is `EVENT_ACTIVITY`, unless it explicitly provides or forms part of an institute wellbeing support service.

Generic `wellness`, `health`, `balance`, `support`, `take care`, or `yoga` language alone is insufficient.

### `RECOGNITION_ACHIEVEMENT`

Requires completed-state evidence and recognition as the primary purpose.

Strong completed-state examples include:

- `wins` or `won`;
- `awarded` or `received`;
- `congratulating` or `congratulations`;
- `secured first place`;
- `honoured`;
- `selected as winner`.

`Award`, `winner`, `prize`, or a prize amount alone is insufficient. Future prize pools, award applications, nomination/application calls, and competition invitations must not be classified as recognition.

### `GENERAL_OTHER`

Use for meaningful messages outside the specialist categories and whenever usable evidence is absent, only generic evidence exists, a tie remains unresolved, or attachment-only meaning cannot be inspected.

Typical V1 examples include campus merchandise orders and hostel auctions. `GENERAL_OTHER` is a normal category, not an error condition.

## 10. Quote handling

1. Score current authored content first.
2. Use quoted previous content only when current content is insufficient, such as a short reminder followed by a complete original announcement.
3. Quote-only classification is capped at `MEDIUM`.
4. Quoted content never overrides conflicting, specific evidence in current authored content.
5. Repeated quoted content and repeated copies of the same logical evidence do not add points.

If current content contains only a neutral reminder such as “deadline today,” the quoted original may supply the category. If current content states a specific new purpose such as “event cancelled” or “service restored,” that current purpose controls.

## 11. Simple negation suppression

V1 suppresses an otherwise positive hit when an obvious nearby negation applies, including forms such as:

- `not an internship`;
- `not a job`;
- `no placement`;
- `non-employment`.

This is a deliberately limited edge-case extension, not a pattern directly observed in the mailbox discovery sample. V1 must not attempt a general NLP negation engine.

## 12. Attachments

Attachment content is outside V1. Attachment names/presence may be retained as metadata but must not substitute for subject/body evidence.

If the subject/body is insufficient and the useful meaning exists only in an attachment, return `GENERAL_OTHER`/`LOW`. This is an accepted V1 limitation.

## 13. Normative decision procedure

1. Accept null-safe subject/body input without changing stored content.
2. Produce substantive visible text from plain text or cleaned HTML.
3. Normalise case, whitespace, common punctuation/hyphens, and evidence-only subject prefixes.
4. Identify current text, quoted history, signature/footer/boilerplate, URLs, addresses, and social links where reasonably possible.
5. Mask zero-point zones from evidence matching.
6. Match approved strong and medium evidence with word/phrase boundaries.
7. Apply simple nearby negation suppression.
8. Deduplicate logical evidence and do not count repetition.
9. Score subject and current body for each category.
10. If current evidence is insufficient, optionally perform quote rescue and cap confidence at `MEDIUM`.
11. Determine the primary purpose using section 8 and the category boundaries in section 9.
12. If a unique supported category remains, return it with the section 7 confidence class/value.
13. If no category is sufficiently supported or a genuine tie remains, return `GENERAL_OTHER`/`LOW` (0.5).
14. Return one primary category only; do not expose a runner-up in V1.

## 14. Approved V1 test plan

All test inputs must remain synthetic/anonymised. Detailed implementation status is recorded in `docs/TEST_LOG.md`; all 20 Test Batch 1 cases are GREEN, while remaining V1 cases are still planned.

### Positive category coverage

| Planned test | Expected | Confidence |
|---|---|---|
| `academic_subject` | `ACADEMIC_RESEARCH` | `HIGH` |
| `academic_body_only` | `ACADEMIC_RESEARCH` | `HIGH` |
| `academic_casefold` | `ACADEMIC_RESEARCH` | `HIGH` |
| `academic_boundary` | `ACADEMIC_RESEARCH` | `HIGH` |
| `career_subject` | `CAREER` | `HIGH` |
| `career_body_only` | `CAREER` | `HIGH` |
| `career_casefold` | `CAREER` | `HIGH` |
| `career_boundary` | `CAREER` | `HIGH` |
| `event_subject` | `EVENT_ACTIVITY` | `HIGH` |
| `event_body_only` | `EVENT_ACTIVITY` | `HIGH` |
| `event_casefold` | `EVENT_ACTIVITY` | `HIGH` |
| `event_boundary` | `EVENT_ACTIVITY` | `HIGH` |
| `call_subject` | `CALL_FOR_PARTICIPATION` | `HIGH` |
| `call_body_only` | `CALL_FOR_PARTICIPATION` | `HIGH` |
| `call_casefold` | `CALL_FOR_PARTICIPATION` | `HIGH` |
| `call_boundary` | `CALL_FOR_PARTICIPATION` | `MEDIUM` |
| `admin_subject` | `ADMINISTRATIVE_NOTICE` | `HIGH` |
| `admin_body_only` | `ADMINISTRATIVE_NOTICE` | `HIGH` |
| `admin_casefold` | `ADMINISTRATIVE_NOTICE` | `HIGH` |
| `admin_boundary` | `ADMINISTRATIVE_NOTICE` | `HIGH` |
| `wellbeing_subject` | `WELLBEING_SUPPORT` | `HIGH` |
| `wellbeing_body_only` | `WELLBEING_SUPPORT` | `HIGH` |
| `wellbeing_casefold` | `WELLBEING_SUPPORT` | `HIGH` |
| `wellbeing_boundary` | `WELLBEING_SUPPORT` | `HIGH` |
| `recognition_subject` | `RECOGNITION_ACHIEVEMENT` | `HIGH` |
| `recognition_body_only` | `RECOGNITION_ACHIEVEMENT` | `HIGH` |
| `recognition_casefold` | `RECOGNITION_ACHIEVEMENT` | `HIGH` |
| `recognition_boundary` | `RECOGNITION_ACHIEVEMENT` | `HIGH` |
| `general_subject` | `GENERAL_OTHER` | `HIGH` |
| `general_body_only` | `GENERAL_OTHER` | `LOW` |
| `general_casefold` | `GENERAL_OTHER` | `LOW` |
| `general_boundary` | `GENERAL_OTHER` | `LOW` |

A clearly identified out-of-taxonomy purpose, such as a hostel bicycle auction, may be `GENERAL_OTHER`/`HIGH`. Fallback caused by no usable evidence, generic-only evidence, attachment-only meaning, or an unresolved tie remains `GENERAL_OTHER`/`LOW`.

### Negative and misleading evidence

| Planned test | Expected | Confidence |
|---|---|---|
| `unrelated_generic` | `GENERAL_OTHER` | `LOW` |
| `opening_not_job` | `ADMINISTRATIVE_NOTICE` | `HIGH` |
| `opportunity_not_career` | `EVENT_ACTIVITY` | `HIGH` |
| `research_in_job` | `CAREER` | `HIGH` |
| `event_in_footer` | `ADMINISTRATIVE_NOTICE` | `HIGH` |
| `career_in_signature` | `ADMINISTRATIVE_NOTICE` | `HIGH` |
| `quote_misleading` | `ADMINISTRATIVE_NOTICE` | `HIGH` |
| `negated_internship` | `CALL_FOR_PARTICIPATION` | `MEDIUM` |
| `negated_event` | `ACADEMIC_RESEARCH` | `MEDIUM` |
| `url_keyword` | `ADMINISTRATIVE_NOTICE` | `MEDIUM` |
| `email_keyword` | `ADMINISTRATIVE_NOTICE` | `HIGH` |
| `prize_not_recognition` | `CALL_FOR_PARTICIPATION` | `HIGH` |
| `award_submission` | `CALL_FOR_PARTICIPATION` | `HIGH` |
| `wellness_politeness` | `ACADEMIC_RESEARCH` | `HIGH` |

### Priority and conflicts

| Planned test | Expected | Confidence |
|---|---|---|
| `career_vs_event` | `CAREER` | `HIGH` |
| `academic_vs_event` | `ACADEMIC_RESEARCH` | `HIGH` |
| `call_vs_event` | `CALL_FOR_PARTICIPATION` | `HIGH` |
| `admin_vs_event` | `ADMINISTRATIVE_NOTICE` | `HIGH` |
| `wellbeing_vs_event` | `WELLBEING_SUPPORT` | `HIGH` |
| `wellness_activity_tie` | `EVENT_ACTIVITY` | `HIGH` |
| `wellness_support_programme` | `WELLBEING_SUPPORT` | `HIGH` |
| `multi_subject_career_call` | `CAREER` | `HIGH` |
| `multi_body_course_call` | `ACADEMIC_RESEARCH` | `MEDIUM` |
| `selective_academic_programme` | `CALL_FOR_PARTICIPATION` | `HIGH` |
| `recognition_vs_academic` | `RECOGNITION_ACHIEVEMENT` | `HIGH` |
| `exact_tie_fallback` | `GENERAL_OTHER` | `LOW` |
| `repeated_phrase_no_boost` | `EVENT_ACTIVITY` | `MEDIUM` |
| `quote_only_rescue` | `CAREER` | `MEDIUM` |

`wellness_activity_tie` uses a normal public yoga/wellness gathering with date/venue and no support-service context. Its approved V1 result is `EVENT_ACTIVITY`, correcting the earlier draft expectation.

### Robustness

| Planned test | Expected | Confidence |
|---|---|---|
| `empty_both` | `GENERAL_OTHER` | `LOW` |
| `empty_subject` | `WELLBEING_SUPPORT` | `MEDIUM` |
| `empty_body` | `CAREER` | `MEDIUM` |
| `very_short` | `ADMINISTRATIVE_NOTICE` | `MEDIUM` |
| `punctuation` | `ACADEMIC_RESEARCH` | `HIGH` |
| `html_conversion` | `ACADEMIC_RESEARCH` | `HIGH` |
| `reply_chain_current` | `ADMINISTRATIVE_NOTICE` | `MEDIUM` |
| `forward_wrapper` | `CAREER` | `HIGH` |
| `mixed_case` | `CALL_FOR_PARTICIPATION` | `HIGH` |
| `unusual_whitespace` | `WELLBEING_SUPPORT` | `HIGH` |
| `unicode_hyphen` | `CAREER` | `HIGH` |
| `long_footer` | `ADMINISTRATIVE_NOTICE` | `HIGH` |
| `attachment_only_meaning` | `GENERAL_OTHER` | `LOW` |
| `strong_subject_empty_message` | supported specialist category | `MEDIUM` |

### Required test assertions

Future JUnit tests must assert, as applicable:

- primary category;
- internal confidence class;
- persisted/API confidence mapping (`0.9`, `0.7`, `0.5`);
- one-category-only output and no runner-up exposure;
- duplicate evidence suppression;
- zero contribution from sender identity and filtered zones;
- quote rescue cap and current-text priority;
- simple negation suppression;
- null/empty safety and attachment-only fallback;
- no mutation of stored subject/body content.

## 15. Acceptance criteria for implementation

V1 implementation is acceptable only when:

- all eight categories and no unapproved category values are represented;
- all planned tests are implemented with synthetic data and pass;
- the scoring, confidence mapping, purpose boundaries, quote handling, filtering, normalisation, negation, and attachment limitations match this document;
- `GENERAL_OTHER`/`LOW` remains reachable for every approved fallback condition;
- no runner-up category is exposed;
- original email content remains unchanged;
- the focused classifier suite and complete Maven/JUnit suite pass.

## 16. Change control

Changes to category values, confidence mappings, precedence, evidence weights, or boundary decisions require a new human-reviewed specification revision. Adding vocabulary that preserves these decisions may be handled during implementation but must be covered by tests and traceability documentation.
