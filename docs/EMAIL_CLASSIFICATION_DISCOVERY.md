# Email Classification Discovery and Validation

> Historical discovery/validation record. The human-approved V1 decisions are now defined in `docs/CLASSIFICATION_SPEC_V1.md`, which supersedes draft recommendations and test expectations in this document where they differ.

## 1. Methodology

This is an analysis/specification artifact only. No classifier, Java source, database schema, Gmail behavior, or JUnit test was changed.

- Mailbox connection: the student's IIT Madras Smail account, through read-only Gmail operations.
- Sampling date: 2026-10-06 (Asia/Calcutta).
- Candidate pool: recent mail excluding Sent, Spam, Trash, Promotions, and Social categories, followed by manual relevance screening.
- Explicit exclusions: authentication/security messages, OTPs, personal conversations, delivery/transaction receipts, commercial newsletters, unrelated platform mail, and obvious spam.
- Final sample: 70 meaningful university messages dated 2026-10-01 through 2026-10-06.
- Discovery set: the first 50 suitable messages after screening.
- Validation set: 20 held-out suitable messages.
- Freeze protocol: validation headers/snippets were visible only for relevance screening and set assignment; validation bodies were not opened and their patterns were not used to construct the taxonomy. The seven-category taxonomy and discovery counts were frozen before validation bodies were inspected.
- Unit of analysis: individual messages. Replies and forwards were retained when they were meaningful because reply/forward handling is a classifier requirement.
- Attachments: attachment presence was noted where visible, but no attachment was opened or read.

The classification target is the message's primary functional purpose, not every topic mentioned in it. For example, a career talk remains `CAREER` even though it is also an event; a facility-closure notice remains `ADMINISTRATIVE_NOTICE` even if the closure is caused by an event.

## 2. Privacy safeguards

- Gmail operations were read-only. No messages were sent, deleted, labelled, archived, forwarded, marked, or otherwise modified.
- No OAuth credentials, access tokens, OTPs, private addresses, phone numbers, message IDs, account identifiers, raw bodies, or attachments were written to the repository.
- All examples below are synthetic composites or materially paraphrased and anonymised.
- Sender descriptions are role types such as “academic office” or “student club,” not personal identities.
- URLs, email addresses, signatures, contact details, and meeting credentials are omitted.
- Aggregate counts are reported, not a per-message mailbox export.

## 3. Discovery-set size and observed patterns

Discovery set size: **50 messages**.

Observed structural patterns:

- Mailing-list prefixes such as bracketed list names appeared frequently but did not describe the message purpose.
- `Re:` and `Fwd:` messages were common. Several current messages were only a one-line reminder followed by a complete quoted announcement.
- Mailing-list footers, social links, signatures, phone numbers, department names, and generic institutional boilerplate were common.
- Some HTML-heavy seminar messages exposed useful detail in rendered HTML/snippets while their plain-text alternative contained almost only a footer.
- Repeated reminders and quoted chains repeated the same keywords many times; repetition did not represent independent evidence.
- Generic words including “opportunity,” “registration,” “session,” “research,” “event,” and “opening” appeared across multiple functional types.
- A message could plausibly serve two purposes: academic workshop/event, career session/event, wellness event/support, or event/call for participation.
- No discovery message contained a trustworthy real example of explicit negation such as “this is not an internship”; negation handling is therefore an edge-case extension, not a mailbox observation.

## 4. Category frequency table at taxonomy freeze

| Frozen category | Count | Share | Common/distinct enough? |
|---|---:|---:|---|
| `EVENT_ACTIVITY` | 11 | 22% | Yes |
| `ACADEMIC_RESEARCH` | 9 | 18% | Yes |
| `CAREER` | 9 | 18% | Yes |
| `CALL_FOR_PARTICIPATION` | 7 | 14% | Yes |
| `ADMINISTRATIVE_NOTICE` | 7 | 14% | Yes |
| `GENERAL_OTHER` | 4 | 8% | Required fallback |
| `WELLBEING_SUPPORT` | 3 | 6% | Distinct, but low-frequency |
| **Total** | **50** | **100%** | |

The post-validation proposal adds `RECOGNITION_ACHIEVEMENT`; it had zero discovery examples and is not retroactively inserted into the frozen discovery counts.

## 5. Discovery categories

### `ACADEMIC_RESEARCH` — 9 discovery messages

1. **Definition:** teaching, assessment, course administration, seminars, scholarly lectures, research workshops, or academic resources.
2. **Typical sender types:** academic office, course platform, seminar list, research office, technical society.
3. **Common subject structures:** “Seminar …”; “Quiz/Circular …”; “Academic integrity …”; “Workshop/Lecture …”; “Classroom discussion …”.
4. **Strong subject indicators:** `seminar`, `quiz`, `course`, `assessment`, `academic integrity`, `lecture`, or `research workshop` when used as the main purpose.
5. **Strong body indicators:** course/slot details, assessment rules, seminar title/speaker, research talk outcomes, course-class context, or hands-on technical instruction.
6. **Weak indicators:** `research`, `session`, `student`, `presentation`, or an academic department name alone.
7. **False-positive risks:** research terms in a job description; “presentation” in a placement talk; academic words in a signature or quoted history.
8. **Common confusions:** `EVENT_ACTIVITY`, `CALL_FOR_PARTICIPATION`, `CAREER`.
9. **Anonymised examples:** “Department seminar: computational methods”; “Second assessment schedule for degree students”; “Technical workshop with lecture and data lab”; “New discussion posted in an algorithms course.”
10. **Implementation case:** sufficiently common and usually distinct if academic-purpose phrases outweigh generic event language.

### `CAREER` — 9 discovery messages

1. **Definition:** employment, internship, placement, graduate-role, hiring, interview-preparation, or explicitly career-development messages.
2. **Typical sender types:** placement cell, departmental placement representative, skill-development cell, employer recruitment team, campus incubator.
3. **Common subject structures:** “Internship opportunity …”; “Placement opportunity …”; “Job opportunity …”; “Graduate role …”; “Careers in …”.
4. **Strong subject indicators:** `internship`, `placement`, `job`, `graduate role`, `hiring`, `trainee engineer`, `pre-placement`.
5. **Strong body indicators:** employer/role, eligibility, compensation or stipend, joining timeline, job description, resume/application process, interview preparation tied to hiring.
6. **Weak indicators:** `opportunity`, `opening`, `apply`, `skills`, company name, or salary-like numbers without employment context.
7. **False-positive risks:** club openings, event registration, fellowships, hackathon prizes, “career” in a signature/link.
8. **Common confusions:** `CALL_FOR_PARTICIPATION`, `EVENT_ACTIVITY`, `ACADEMIC_RESEARCH`.
9. **Anonymised examples:** “Summer internship in chip design”; “Off-campus graduate engineer role”; “Full-stack developer application reminder”; “Virtual session on quantitative careers.”
10. **Implementation case:** common and high-value; should be implemented as one broad career category before considering internship/placement/job subtypes.

### `EVENT_ACTIVITY` — 11 discovery messages

1. **Definition:** an invitation, screening, exhibition, club activity, class, cultural/religious gathering, competition viewing, or other scheduled gathering whose main purpose is attendance.
2. **Typical sender types:** student club, cultural body, media/sports club, student-affairs office, external community routed by an institute office.
3. **Common subject structures:** “Invitation …”; “Screening …”; “Tutorial …”; “Exhibition …”; date/time/venue separated by punctuation.
4. **Strong subject indicators:** `screening`, `exhibition`, `tutorial`, `invitation`, or a named activity plus date/time/venue.
5. **Strong body indicators:** explicit date, time, venue/mode, “join/attend,” audience description, and event programme.
6. **Weak indicators:** `event`, `session`, `join`, `register`, or a date alone.
7. **False-positive risks:** academic seminar, career talk, recruitment audition, service closure caused by an event.
8. **Common confusions:** every specialist category, especially `CALL_FOR_PARTICIPATION` and `ACADEMIC_RESEARCH`.
9. **Anonymised examples:** “Campus race screening at the lecture theatre”; “Traditional dance tutorial”; “Two-day craft exhibition”; “Evening dialogue on social thought.”
10. **Implementation case:** most frequent discovery type, but needs purpose-specific precedence rules to avoid swallowing other categories.

### `CALL_FOR_PARTICIPATION` — 7 discovery messages

1. **Definition:** competitive or selective calls asking students to apply, audition, submit, register for a limited programme, join a team, or take a club/project role, excluding employment.
2. **Typical sender types:** student club, cultural body, global-engagement office, competition organiser, skill-development cell.
3. **Common subject structures:** “Call for …”; “Applications open …”; “Selections/Auditions …”; “Registrations open …”; deadline reminder.
4. **Strong subject indicators:** `call for`, `applications open`, `auditions`, `selections`, `submit`, `registration deadline`, `hackathon` with participation language.
5. **Strong body indicators:** eligibility, application form, deliverable/audition, selection process, limited places, submission deadline.
6. **Weak indicators:** `apply`, `register`, `deadline`, Google Form, WhatsApp group, or `opportunity` alone.
7. **False-positive risks:** career applications; ordinary event RSVPs; administrative surveys; merchandise order forms.
8. **Common confusions:** `CAREER`, `EVENT_ACTIVITY`, `ADMINISTRATIVE_NOTICE`.
9. **Anonymised examples:** “Applications for writing-club coordinators”; “Freshers’ cultural-course selection”; “Technology build-day registrations extended”; “Global fellowship nomination call.”
10. **Implementation case:** sufficiently common, provided career evidence and ordinary attendance registration take precedence where appropriate.

### `ADMINISTRATIVE_NOTICE` — 7 discovery messages

1. **Definition:** rules, official circulars, schedules, closures, outages, resumption notices, governance notices, facility/service changes, or operational updates.
2. **Typical sender types:** administration, academic office when issuing a circular, student legislature, gymkhana/facility committee, service operator.
3. **Common subject structures:** “Circular …”; “… protocol”; “Temporary unavailability …”; “… will resume”; “List/schedule of …”.
4. **Strong subject indicators:** `circular`, `protocol`, `closed/closure`, `unavailable`, `resume`, `schedule`, `holiday`, `service update`.
5. **Strong body indicators:** mandatory instructions, official policy language, operating status, affected facility, effective period, action required for compliance.
6. **Weak indicators:** `important`, `notice`, `update`, `schedule`, `deadline`, or an official sender alone.
7. **False-positive risks:** event schedules, assessment schedules, application updates, “office” in a signature.
8. **Common confusions:** `ACADEMIC_RESEARCH`, `EVENT_ACTIVITY`, `CALL_FOR_PARTICIPATION`.
9. **Anonymised examples:** “Identity-card protocol circular”; “Public-holiday list”; “Dining coupon service temporarily unavailable”; “Campus service survey delayed.”
10. **Implementation case:** common and distinct when the primary purpose is governance or service status.

### `WELLBEING_SUPPORT` — 3 discovery messages

1. **Definition:** counselling, mental-health support, wellness access, stress support, or institute wellbeing initiatives.
2. **Typical sender types:** wellness centre, peer-support team, dean-students wellness office.
3. **Common subject structures:** “Wellness staff visits …”; “Support during assessments …”; “Yoga for wellness …”.
4. **Strong subject indicators:** `counselling`, `mental health`, `wellness support`, `wellness centre`, or named support access.
5. **Strong body indicators:** licensed counsellors, appointment/walk-in access, stress/concern support, helpline/service availability, explicit wellbeing purpose.
6. **Weak indicators:** `wellness`, `health`, `balance`, `support`, `take care`, or `yoga` alone.
7. **False-positive risks:** generic polite wishes; health-related academic research; a social event branded as wellness.
8. **Common confusions:** `EVENT_ACTIVITY`, `ACADEMIC_RESEARCH`.
9. **Anonymised examples:** “Counsellor visits to residences and departments”; “Peer-support message during examinations”; “Free guided wellbeing practice for the campus.”
10. **Implementation case:** low-frequency but meaningfully distinct and potentially important; human approval is required on whether event-like wellness activities belong here or under events.

### `GENERAL_OTHER` — 4 discovery messages

1. **Definition:** valid university messages that lack sufficient evidence for a specialist category.
2. **Typical sender types:** any campus office, hostel representative, club, or mailing-list contributor.
3. **Common subject structures:** no stable structure.
4. **Strong indicators:** absence of sufficient specialist evidence; campus commerce/merchandise/auction messages were the recurring discovery examples.
5. **Strong body indicators:** a clear primary purpose outside the proposed specialist categories.
6. **Weak indicators:** every isolated generic keyword.
7. **False-positive risks:** using `GENERAL_OTHER` too early can create false negatives; forcing a specialist category creates false positives.
8. **Common confusions:** all categories.
9. **Anonymised examples:** “Hostel bicycle auction”; “Official club merchandise order”; “General campus information without a specialist purpose.”
10. **Implementation case:** mandatory fallback. It is not an error class and must remain reachable.

## 6. Evidence and proposed deterministic rules

“Observed” means the pattern occurred in the mailbox sample. “Recommendation” means Codex inferred a rule suitable for a first deterministic Java classifier; it is not yet an approved implementation decision.

| Evidence/rule | Source | Proposed treatment |
|---|---|---|
| Mailing-list tags, `Re:`, and `Fwd:` are frequent | Observed | Normalise them for matching but preserve the original text for audit/debug output. |
| Current reminder text can be short while a quoted announcement contains the details | Observed | Score current authored text first; use quoted text only when current text is insufficient, and cap confidence at Medium. |
| Signatures, mailing-list footers, social links, and institutional boilerplate contain misleading terms | Observed | Segment and ignore them for evidence scoring where feasible. |
| Plain-text MIME alternatives can be nearly empty while HTML contains the announcement | Observed | Prefer cleaned plain text when substantive; otherwise use safely converted HTML text. |
| Repeated quoted chains duplicate keywords | Observed | Count distinct evidence features, not raw occurrences. |
| Generic `opportunity`, `opening`, `event`, `research`, `session`, `apply`, and `register` cross categories | Observed | Never classify from one generic word alone. |
| Phone numbers, email addresses, and URLs may contain misleading tokens | Observed/inferred | Remove or mask them before lexical evidence matching. |
| Negated category phrases were not observed | Edge-case inference | Suppress a hit within a small negation window such as `not/no/non-<category>`; test before implementation. |
| Case, punctuation, hyphens, and plural variants are inconsistent | Observed/inferred | Lowercase with Unicode-aware normalisation and match phrases/word boundaries, not arbitrary substrings. |
| Subject and current body can disagree | Observed ambiguity | Treat corroboration as High; treat a conflict as Medium/Low and apply an approved purpose precedence or fallback. |

### Recommended scoring sketch (not approved)

- Strong subject phrase: 3 evidence points.
- Strong current-body phrase: 2 evidence points.
- Medium contextual phrase: 1 evidence point.
- Sender type: corroboration only; never enough by itself.
- Repeated identical phrase: counted once per logical section.
- Quoted-only evidence: at most Medium confidence.
- Footer/signature/URL/email evidence: zero points.
- High confidence: subject and current body corroborate, or at least two independent strong current-message indicators exist.
- Medium confidence: one strong indicator plus compatible context, or useful evidence is quote-only.
- Low confidence: conflict, tie, generic-only evidence, or fallback.

Recommended purpose precedence, subject to human approval:

1. `CAREER` over generic call/event evidence when an employment role, employer, eligibility, compensation, or resume process is explicit.
2. `ADMINISTRATIVE_NOTICE` over event language when the primary act is a closure, outage, resumption, policy, or mandatory instruction.
3. `ACADEMIC_RESEARCH` over event language when the primary act is teaching, assessment, seminar, scholarly lecture, or research workshop.
4. `WELLBEING_SUPPORT` over event language when access to support/services or explicit mental-health help is primary.
5. `CALL_FOR_PARTICIPATION` over `EVENT_ACTIVITY` when selection, submission, audition, or team/role application is primary.
6. `RECOGNITION_ACHIEVEMENT` only for completed achievements, never merely a future prize or competition call.
7. An unresolved top-score tie falls back to `GENERAL_OTHER` with Low confidence rather than using arbitrary enum order.

## 7. Ambiguity and adversarial observations

- **Academic event vs ordinary event:** research seminars and technical workshops include date/time/venue, but their primary purpose is scholarly learning.
- **Career event vs ordinary event:** career sessions have event structure; explicit careers/roles/interview evidence makes them `CAREER`.
- **Call vs event:** a hackathon schedule update both announces an event and solicits participants. Selection/registration as the primary action supports `CALL_FOR_PARTICIPATION`.
- **Wellbeing vs event:** a scheduled wellness practice can be either; the discovery draft assigned explicit institute wellness initiatives to `WELLBEING_SUPPORT`.
- **Administrative vs call:** feedback surveys request participation but primarily concern operation of a campus service.
- **Subject/body mismatch:** generic or sparse subjects sometimes rely entirely on body text; forwarded subjects can be stronger than the wrapper body.
- **Replies/forwards:** one-line reminders often quote the original; blindly scoring the entire chain inflates repeated keywords.
- **Misleading links/addresses:** category-looking words inside URLs/email addresses are not semantic evidence.
- **Abbreviations:** sample abbreviations included institutional and programme/degree abbreviations, but no stable category abbreviations were sufficiently reliable alone.
- **No-fit messages:** merchandise orders and hostel auctions legitimately remained `GENERAL_OTHER`.

## 8. False-positive and false-negative risks

Major false-positive risks:

- Classifying every `opportunity` or `opening` as `CAREER`.
- Classifying any dated message as `EVENT_ACTIVITY`.
- Treating any form or `register/apply` phrase as `CALL_FOR_PARTICIPATION`.
- Treating research terms in job descriptions as `ACADEMIC_RESEARCH`.
- Treating prize pools or “award available” in future competitions as `RECOGNITION_ACHIEVEMENT`.
- Scoring signatures, mailing-list footers, quoted history, URLs, or email addresses as current-message evidence.
- Letting repeated quoted keywords overpower a clear current-message purpose.

Major false-negative risks:

- Sparse/empty subjects whose only evidence is in the body.
- HTML-only content when plain text is only a mailing-list footer.
- Unseen abbreviations, spelling errors, Unicode punctuation, or mixed-language wording.
- Career messages using only a role name without `job`, `placement`, or `internship`.
- Wellbeing messages framed as invitations without explicit support vocabulary.
- Recognition messages without `wins/awarded/congratulating` phrasing.
- A conservative fallback threshold sending legitimate specialist messages to `GENERAL_OTHER`.

## 9. Validation results under the frozen taxonomy

Validation set size: **20 messages**.

| Frozen-category result | Count |
|---|---:|
| `GENERAL_OTHER` | 7 |
| `CALL_FOR_PARTICIPATION` | 6 |
| `ACADEMIC_RESEARCH` | 2 |
| `ADMINISTRATIVE_NOTICE` | 2 |
| `EVENT_ACTIVITY` | 2 |
| `WELLBEING_SUPPORT` | 1 |
| `CAREER` | 0 |
| **Total** | **20** |

Outcome summary:

- Clean classifications: **12/20 (60%)**.
- Ambiguous classifications: **2/20 (10%)** — an online academic-course registration (`ACADEMIC_RESEARCH` vs `CALL_FOR_PARTICIPATION`) and a campus-facility feedback request (`ADMINISTRATIVE_NOTICE` vs `CALL_FOR_PARTICIPATION`).
- Definite wrong specialist classifications: **0/20** when `GENERAL_OTHER` is treated as a valid fallback.
- Under-specific/failure-to-model results: **6/20 (30%)**, all completed institutional achievement/award announcements that fell to `GENERAL_OTHER`.
- Messages indicating a missing category: **6/20**, all supporting the same candidate `RECOGNITION_ACHIEVEMENT` type.
- Identifiable false positives: **0** in manual validation.
- Identifiable false negatives: **6 recognition messages** relative to the revised proposal.

### Post-validation change record

| What failed | Why | Proposed change | Risk introduced |
|---|---|---|---|
| Six completed award/achievement announcements fell to `GENERAL_OTHER` | This pattern was absent from the discovery sample but repeated coherently in validation | Add `RECOGNITION_ACHIEVEMENT` | `award`, `winner`, or `prize` in a future competition/call could be misclassified |
| Online-course registration was split between academic content and application action | Functional purpose and call-to-action differ | Keep `ACADEMIC_RESEARCH` when a structured course is the object; lower confidence if selection/admission is central | May under-classify competitive academic programmes as calls |
| Facility feedback request was split between administration and participation | A survey is both an operational feedback notice and a call to respond | Keep service/facility feedback under `ADMINISTRATIVE_NOTICE`; ordinary research/club surveys remain `GENERAL_OTHER` unless later evidence supports a survey category | Broad admin rules could absorb unrelated surveys |

The original taxonomy was not silently changed. With the proposed recognition category, the same validation set would yield 18 clean classifications and 2 ambiguities (90% clean, 10% ambiguous), but that is a retrospective estimate, not an implemented/tested accuracy claim.

## 10. Proposed classifier specification after validation

### Proposed final categories

1. `ACADEMIC_RESEARCH`
2. `CAREER`
3. `EVENT_ACTIVITY`
4. `CALL_FOR_PARTICIPATION`
5. `ADMINISTRATIVE_NOTICE`
6. `WELLBEING_SUPPORT`
7. `RECOGNITION_ACHIEVEMENT` (post-validation addition)
8. `GENERAL_OTHER`

### Category criteria

| Category | Inclusion | Exclusion | High-confidence evidence | Medium evidence | Ignore | Main ambiguity/fallback |
|---|---|---|---|---|---|---|
| `ACADEMIC_RESEARCH` | Course, assessment, seminar, scholarly talk/workshop, academic resource | Jobs, general attendance events, completed awards | Explicit seminar/course/quiz/assessment plus academic detail | Lecture/research/session with academic sender/context | Department/signature alone | Prefer specialist purpose; otherwise `GENERAL_OTHER` |
| `CAREER` | Job, internship, placement, hiring, graduate role, career preparation | Club positions, fellowships without employment, competitions | Role/employer plus eligibility/compensation/resume/application | `career`, `hiring`, role title with supporting context | `opportunity/opening` alone | Career overrides call/event when employment is explicit |
| `EVENT_ACTIVITY` | Scheduled gathering primarily asking recipients to attend | Assessments, jobs, selections, outages, support access | Named activity plus date/time/venue and attend/join language | Invitation/session/screening with partial logistics | Date or `event` alone | Loses to a more specific primary purpose |
| `CALL_FOR_PARTICIPATION` | Apply/audition/submit/register for team, role, fellowship, contest, hackathon | Employment, ordinary RSVP, surveys/orders | Selection/application action plus eligibility/deadline/deliverable | `call/apply/register` plus limited-programme context | Form link alone | Career first; attendance-only event stays event |
| `ADMINISTRATIVE_NOTICE` | Policy, circular, closure, outage, operation, official schedule, facility feedback | Event schedule, assessment schedule, recruitment | Mandatory/operational phrase plus affected service/rule | Official update with concrete operational effect | Official sender alone | Primary service/governance purpose controls |
| `WELLBEING_SUPPORT` | Counselling, mental-health service, stress support, institute wellness access | Academic health research, generic wishes, ordinary fitness event | Counselling/support service plus access details | Explicit wellness initiative and health purpose | `support/wellness` alone | Human decision needed for yoga/meditation events |
| `RECOGNITION_ACHIEVEMENT` | Completed award, prize, win, honour, or achievement announcement | Future awards, prize pools, contest invitations, application calls | Past-tense `wins/awarded/congratulating` plus recipient and completed honour | `winner/award` with clear completed-event context | `award/prize` alone | Completed state is mandatory; otherwise call/event/general |
| `GENERAL_OTHER` | Meaningful message without sufficient specialist evidence | None | Clear out-of-taxonomy purpose or no specialist threshold met | Conflicted/tied evidence | N/A | Required result for ties, generic-only, or insufficient evidence |

### Preprocessing and decision flow (recommended, not approved)

1. Null-safe subject/body handling.
2. Decode/convert HTML to visible text; prefer substantive plain text, otherwise cleaned HTML.
3. Normalise Unicode case and whitespace; remove list/reply/forward subject prefixes for evidence matching.
4. Segment current authored content, quoted history, signature, footer, URLs, and email addresses.
5. Match phrase lists with token/word boundaries and simple inflection/hyphen variants.
6. Suppress negated hits and deduplicate repeated evidence.
7. Score current subject and current body; use quote-only evidence only as a lower-confidence rescue.
8. Apply approved purpose precedence.
9. Return category, confidence class, and matched evidence identifiers (not private body text).
10. Return `GENERAL_OTHER`/Low for an unresolved tie or insufficient evidence.

## 11. Proposed test matrix

All inputs below are synthetic/anonymised. “Observed” means derived from a mailbox pattern; “Extension” means deliberately designed robustness/adversarial coverage. The expected results assume the post-validation proposal and the recommended precedence; they require human approval before implementation.

### Per-category positive cases

| Test name | Synthetic input | Expected | Confidence | Reason | Origin |
|---|---|---|---|---|---|
| academic_subject | S: `Department Seminar: Reliable Systems`; B: date, speaker, abstract | `ACADEMIC_RESEARCH` | High | Explicit seminar and scholarly detail | Observed |
| academic_body_only | S: `Update`; B: `The second quiz for degree students begins Monday; slot schedule follows.` | `ACADEMIC_RESEARCH` | High | Assessment evidence is body-only | Observed |
| academic_casefold | S: `aCaDeMiC InTeGrItY in ASSESSMENTS`; B: policy context | `ACADEMIC_RESEARCH` | High | Case-insensitive phrase | Extension |
| academic_boundary | S: `Course-workshop: data analysis`; B: hands-on scholarly instruction | `ACADEMIC_RESEARCH` | High | Hyphen/punctuation variant | Observed/extension |
| career_subject | S: `Summer Internship Opportunity — Robotics`; B: employer, stipend, eligibility | `CAREER` | High | Explicit internship/employment evidence | Observed |
| career_body_only | S: `Applications`; B: `Graduate engineer role; submit resume; compensation stated.` | `CAREER` | High | Role/process in body | Observed |
| career_casefold | S: `OFF-CAMPUS PLACEMENT opportunity`; B: company and role | `CAREER` | High | Case-insensitive placement | Observed/extension |
| career_boundary | S: `Pre‑placement talk / graduate-role`; B: hiring application follows | `CAREER` | High | Unicode hyphen and phrase variants | Extension |
| event_subject | S: `Film Screening | Friday | 7 PM | Hall A`; B: invitation | `EVENT_ACTIVITY` | High | Named activity plus logistics | Observed |
| event_body_only | S: `Invitation`; B: `Join the exhibition on Friday at the community hall.` | `EVENT_ACTIVITY` | High | Attendance/logistics in body | Observed |
| event_casefold | S: `CAMPUS EXHIBITION this weekend`; B: time and venue | `EVENT_ACTIVITY` | High | Case-insensitive event phrase | Extension |
| event_boundary | S: `Dance tutorial—03 Oct, 8 p.m.`; B: `All may attend.` | `EVENT_ACTIVITY` | High | Punctuation/time variant | Observed/extension |
| call_subject | S: `Call for Club Coordinators`; B: eligibility and deadline | `CALL_FOR_PARTICIPATION` | High | Selective role application | Observed |
| call_body_only | S: `Team update`; B: `Auditions are open; submit the form by Friday.` | `CALL_FOR_PARTICIPATION` | High | Selection action in body | Observed |
| call_casefold | S: `APPLICATIONS OPEN: student project`; B: selection criteria | `CALL_FOR_PARTICIPATION` | High | Case-insensitive phrase | Observed/extension |
| call_boundary | S: `Hack-a-thon registrations`; B: team eligibility and submission | `CALL_FOR_PARTICIPATION` | Medium | Spelling/hyphen variant plus context | Extension |
| admin_subject | S: `Circular: Laboratory Access Protocol`; B: mandatory instructions | `ADMINISTRATIVE_NOTICE` | High | Official operational rule | Observed |
| admin_body_only | S: ``; B: `The dining coupon service is unavailable for 48 hours.` | `ADMINISTRATIVE_NOTICE` | High | Empty subject; clear outage | Observed pattern/extension |
| admin_casefold | S: `SWIMMING POOL WILL RESUME`; B: effective date | `ADMINISTRATIVE_NOTICE` | High | Case-insensitive operation status | Observed/extension |
| admin_boundary | S: `Service re-opening / revised hours`; B: facility details | `ADMINISTRATIVE_NOTICE` | High | Word-boundary/inflection variant | Extension |
| wellbeing_subject | S: `Counselling and Mental-Health Support`; B: access hours | `WELLBEING_SUPPORT` | High | Explicit support service | Observed |
| wellbeing_body_only | S: `For students`; B: `Licensed counsellors offer walk-in stress support.` | `WELLBEING_SUPPORT` | High | Service evidence in body | Observed |
| wellbeing_casefold | S: `WELLNESS CENTRE appointments`; B: counselling details | `WELLBEING_SUPPORT` | High | Case-insensitive support evidence | Observed/extension |
| wellbeing_boundary | S: `Well-being support schedule`; B: appointment locations | `WELLBEING_SUPPORT` | High | Hyphenated variant | Extension |
| recognition_subject | S: `Student team wins Best Design Award`; B: completed competition | `RECOGNITION_ACHIEVEMENT` | High | Past-tense completed achievement | Observed in validation |
| recognition_body_only | S: `Institute announcement`; B: `Please congratulate the team on being awarded first place.` | `RECOGNITION_ACHIEVEMENT` | High | Congratulatory completed result | Observed in validation |
| recognition_casefold | S: `SCHOLAR AWARDED best paper`; B: completed conference | `RECOGNITION_ACHIEVEMENT` | High | Case-insensitive award phrase | Observed/extension |
| recognition_boundary | S: `Winner’s Award—completed challenge`; B: recipient and result | `RECOGNITION_ACHIEVEMENT` | High | Apostrophe/punctuation variant | Observed/extension |
| general_subject | S: `Hostel Bicycle Auction`; B: bidding instructions | `GENERAL_OTHER` | High | Clear purpose outside specialist set | Observed |
| general_body_only | S: `Information`; B: `Orders are open for club T-shirts and sizes.` | `GENERAL_OTHER` | Medium | Merchandise is outside specialist set | Observed |
| general_casefold | S: `OFFICIAL MERCHANDISE ORDER`; B: price and size form | `GENERAL_OTHER` | High | Case normalisation must not force call/career | Observed/extension |
| general_boundary | S: `Cycle-auction details`; B: bidding and collection | `GENERAL_OTHER` | High | Hyphenated out-of-taxonomy purpose | Observed/extension |

### Negative and misleading-evidence cases

| Test name | Synthetic input | Expected | Confidence | Reason | Origin |
|---|---|---|---|---|---|
| unrelated_generic | S: `Weekly note`; B: `Thanks for your cooperation.` | `GENERAL_OTHER` | Low | No specialist evidence | Extension |
| opening_not_job | S: `Library opening hours revised`; B: new operating hours | `ADMINISTRATIVE_NOTICE` | High | Generic `opening` is operational, not career | Extension |
| opportunity_not_career | S: `Opportunity to attend a film screening`; B: date/venue only | `EVENT_ACTIVITY` | High | Attendance, no employment evidence | Observed/extension |
| research_in_job | S: `Graduate analyst role`; B: `research skills`, salary, resume | `CAREER` | High | Job purpose overrides generic research | Observed |
| event_in_footer | S: `Service unavailable`; B: outage, then footer `events.example` | `ADMINISTRATIVE_NOTICE` | High | Footer/URL ignored | Extension |
| career_in_signature | S: `Pool reopened`; B: operation note; signature `Career Office alumnus` | `ADMINISTRATIVE_NOTICE` | High | Signature ignored | Extension |
| quote_misleading | S: `Re: Pool status`; B: `Pool reopens today.` Quote: `Internship applications open.` | `ADMINISTRATIVE_NOTICE` | High | Current text outranks quote | Extension |
| negated_internship | S: `Volunteer project`; B: `This is not an internship or job; unpaid club service.` | `CALL_FOR_PARTICIPATION` | Medium | Negated career hit suppressed | Extension |
| negated_event | S: `Online resource`; B: `This is not an event; materials are available asynchronously.` | `ACADEMIC_RESEARCH` | Medium | Negated event hit suppressed; course resource remains | Extension |
| url_keyword | S: `Facility feedback`; B: `Form: https://career.example/survey`; service feedback context | `ADMINISTRATIVE_NOTICE` | Medium | URL token is ignored | Extension |
| email_keyword | S: `Holiday circular`; B: policy; contact `research-office@example.edu` | `ADMINISTRATIVE_NOTICE` | High | Address token ignored | Extension |
| prize_not_recognition | S: `Hackathon registrations — prize pool available`; B: future contest | `CALL_FOR_PARTICIPATION` | High | Future prize is not completed achievement | Validation-risk extension |
| award_submission | S: `Submit for the student award`; B: applications close Friday | `CALL_FOR_PARTICIPATION` | High | Future award application is a call | Validation-risk extension |
| wellness_politeness | S: `Quiz schedule`; B: timetable and `take care of your wellbeing` | `ACADEMIC_RESEARCH` | High | Polite wellness phrase is weak | Observed |

### Priority and conflict cases

| Test name | Synthetic input | Expected | Confidence | Reason | Origin |
|---|---|---|---|---|---|
| career_vs_event | S: `Pre-placement talk`; B: date/venue plus graduate-role application | `CAREER` | High | Employment purpose overrides event form | Observed |
| academic_vs_event | S: `Research seminar`; B: speaker, abstract, date/venue | `ACADEMIC_RESEARCH` | High | Scholarly purpose overrides event form | Observed |
| call_vs_event | S: `Auditions Friday`; B: selection criteria, form, venue | `CALL_FOR_PARTICIPATION` | High | Selection is primary | Observed |
| admin_vs_event | S: `Stadium closed during walkathon`; B: closure hours | `ADMINISTRATIVE_NOTICE` | High | Operational effect is primary | Observed in validation |
| wellbeing_vs_event | S: `Counselling access day`; B: licensed support and appointments | `WELLBEING_SUPPORT` | High | Support service is primary | Observed |
| wellness_activity_tie | S: `Yoga for wellness`; B: one-hour public practice with date/venue and no support-service context | `EVENT_ACTIVITY` | High | Human-approved V1 boundary: ordinary attendance activity is an event | Observed / V1-approved |
| multi_subject_career_call | S: `Internship applications open`; B: employer, stipend, deadline | `CAREER` | High | Explicit employment outranks call | Observed |
| multi_body_course_call | S: `Registration open`; B: self-paced academic course, no selection | `ACADEMIC_RESEARCH` | Medium | Course object outweighs generic registration | Observed in validation |
| recognition_vs_academic | S: `Scholar wins best paper`; B: completed conference research | `RECOGNITION_ACHIEVEMENT` | High | Achievement, not the paper topic, is purpose | Observed in validation |
| exact_tie_fallback | S: `Opportunity update`; B: equal independent event and club-application evidence | `GENERAL_OTHER` | Low | No arbitrary enum-order tie-break | Extension |
| repeated_phrase_no_boost | S: `Update`; B: `internship` repeated ten times in a quoted footer; current text says exhibition | `EVENT_ACTIVITY` | Medium | Duplicate/quoted hits do not accumulate | Extension |
| quote_only_rescue | S: `Re: Reminder`; B: `Deadline today.` Quote: complete job announcement | `CAREER` | Medium | Quote used only because current segment is insufficient | Observed |

### Robustness cases

| Test name | Synthetic input | Expected | Confidence | Reason | Origin |
|---|---|---|---|---|---|
| empty_both | S: ``; B: `` | `GENERAL_OTHER` | Low | Null-safe fallback | Extension |
| empty_subject | S: ``; B: `Counselling appointments are available.` | `WELLBEING_SUPPORT` | Medium | Body-only classification | Observed pattern/extension |
| empty_body | S: `Summer internship opportunity`; B: `` | `CAREER` | Medium | Strong subject only | Extension |
| very_short | S: `Reminder`; B: `Pool reopened.` | `ADMINISTRATIVE_NOTICE` | Medium | Short but specific operation cue | Observed |
| punctuation | S: `[NOTICE]—QUIZ-II: schedule`; B: slot details | `ACADEMIC_RESEARCH` | High | Punctuation/list prefix normalisation | Observed/extension |
| html_conversion | S: `Seminar`; B: `<div>Speaker &amp; abstract<br>Date</div>` | `ACADEMIC_RESEARCH` | High | Visible HTML text is classified | Observed |
| reply_chain_current | S: `Re: Event`; B: `Cancelled.` Quote: original invitation | `ADMINISTRATIVE_NOTICE` | Medium | Current cancellation controls | Extension |
| forward_wrapper | S: `Fwd: Graduate role`; B: forwarding line then job details | `CAREER` | High | Forward prefix ignored, content retained | Observed |
| mixed_case | S: `cAlL FoR AuDiTiOnS`; B: eligibility | `CALL_FOR_PARTICIPATION` | High | Case-insensitive matching | Extension |
| unusual_whitespace | S: `Mental\t health\n support`; B: `walk-in   counselling` | `WELLBEING_SUPPORT` | High | Whitespace normalisation | Extension |
| unicode_hyphen | S: `Pre‑placement graduate‑role`; B: hiring details | `CAREER` | High | Unicode punctuation normalisation | Extension |
| long_footer | S: `Holiday circular`; B: policy followed by long social/mailing-list footer | `ADMINISTRATIVE_NOTICE` | High | Boilerplate does not dilute/redirect evidence | Observed |

## 12. Limitations

- This is one mailbox, one short time window, and one institution; distributions may shift by semester, recruitment cycle, examinations, or festival calendar.
- The sampling process was purposive after exclusions, not a statistically random mailbox sample.
- Validation metadata/snippets had to be visible for relevance screening before freeze; only the validation bodies and classification evidence were held back. This is a mild leakage risk and should be improved in a later labelled-data workflow.
- Individual messages, rather than deduplicated threads, were sampled; reminders and forwards therefore reflect real ingestion conditions but are not independent events.
- Attachment-only meaning was intentionally not inspected, so messages whose category exists only inside an attachment can become false negatives.
- No actual deterministic classifier was executed; validation is a manual specification exercise, not a measured model benchmark.
- Confidence thresholds and precedence are proposals, not calibrated probabilities.
- Negation, misspelling, and some adversarial tests are synthetic extensions because the sample did not provide enough real instances.
- Sender identity can help corroborate evidence but must not become a brittle allow-list.

## 13. AI decision traceability and human approval gate

### ACCEPTED recommendation (for this draft specification only)

- Keep a reachable `GENERAL_OTHER` fallback and do not force specialised categories.
- Prefer a compact first version rather than splitting career into internships/jobs/placements or events into many subtypes.
- Treat subject, current body, quotes, signatures/footers, and URLs as separate evidence zones.
- Count independent features rather than raw keyword frequency.
- Preserve the frozen discovery taxonomy and show validation-driven changes separately.

These acceptances authorise documentation only, not Java implementation.

### REJECTED recommendation

- Keyword-only category selection: rejected because generic terms crossed categories and appeared in boilerplate.
- Raw-occurrence frequency boosting: rejected because replies/forwards repeated quoted text.
- Forced specialist classification: rejected because auctions/merchandise genuinely required a fallback.
- Sender-only classification: rejected because mailing-list wrappers hide the originating sender and sender roles are not perfectly stable.

### DEFERRED recommendation

- Split `CAREER` into internship, placement, job, and career-development subcategories: insufficient need for version one.
- Add separate commerce, survey, sports, cultural, religious, or facility subcategories: sample size and separability do not justify them yet.
- Use ML/NLP, embeddings, semantic ranking, or behavioural learning: outside the current milestone.
- Inspect attachment content: deferred for privacy and scope reasons.

### REQUIRES HUMAN DECISION before implementation

1. Approve the seven frozen categories and the post-validation addition of `RECOGNITION_ACHIEVEMENT`.
2. Approve any merge/removal, especially whether `WELLBEING_SUPPORT` stays separate at 6% discovery frequency.
3. Approve the purpose precedence order and whether unresolved ties return `GENERAL_OTHER`.
4. Decide exact subject-vs-body weights and whether a strong subject can classify an empty body.
5. Approve High/Medium/Low confidence definitions and API representation.
6. Decide quote handling: ignore by default, or use as a Medium-confidence rescue for short reminders.
7. Approve signature/footer/URL/email stripping heuristics and acceptable failure behavior.
8. Decide whether wellness-branded yoga/meditation gatherings are `WELLBEING_SUPPORT` or `EVENT_ACTIVITY`.
9. Decide whether facility feedback surveys are `ADMINISTRATIVE_NOTICE` or `CALL_FOR_PARTICIPATION`.
10. Decide whether structured academic course registration is `ACADEMIC_RESEARCH` or `CALL_FOR_PARTICIPATION`.
11. Approve word/phrase lists, negation window, Unicode normalisation, and deduplication rules.
12. Approve returning one category only versus exposing an ambiguity flag/runner-up category.
13. Approve the fallback behavior for ties, no evidence, empty input, and attachment-only messages.

**STATUS UPDATE:** the decisions above were resolved in the human-approved `docs/CLASSIFICATION_SPEC_V1.md`. Classifier implementation and JUnit creation remain not started.
