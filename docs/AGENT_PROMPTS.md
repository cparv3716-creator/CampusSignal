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
