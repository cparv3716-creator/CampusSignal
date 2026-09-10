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

## Prompt Entry
Date: 2026-09-08
Contributor: Priyanshi Garg
Branch: feature/java-springboot-migration
Milestone: 11 September 2026 — Email Retrieval and User Preference Setup
Task: Java/Spring Boot Migration

### Prompt
# Context from my IDE setup:

## Active file: docs/WORK_LOG.md

## Open tabs:
- WORK_LOG.md: docs/WORK_LOG.md
- AGENT_PROMPTS.md: docs/AGENT_PROMPTS.md
- AGENTS.md: AGENTS.md

## My request:
You are working on the CampusSignal repository.

IMPORTANT CORRECTION:
The current repository contains a Python/FastAPI prototype, but the submitted CS5013 proposal requires the project backend to use Java, Maven, and Spring Boot.

Your task is to begin the migration to the correct Java stack and implement the 11 September milestone in Java.

Do NOT delete the existing Python implementation yet. Treat it only as a behavioral reference until the equivalent Java functionality is working and tested.

\==================================================
SOURCE OF TRUTH
\==================================================

The submitted proposal requires:

Language/build:
\- Java
\- Maven

Backend:
\- Spring Boot

Email access:
\- Gmail API
\- Google OAuth 2.0

Database:
\- relational database

11 September 2026 milestone:
\- Gmail API integration with OAuth 2.0
\- retrieve emails from an authorized account
\- store:
&#x20; \- message ID
&#x20; \- subject
&#x20; \- sender
&#x20; \- timestamp
&#x20; \- body
\- basic duplicate handling
\- user profiles containing:
&#x20; \- programme
&#x20; \- year
&#x20; \- selected interests
\- initial category-score model so different users can have different preference scores for the same categories

Do NOT implement the October AI/NLP milestone in this task.

\==================================================
BEFORE CHANGING CODE
\==================================================

1\. Inspect the full repository.
2\. Inspect:
&#x20;  \- current Python Gmail implementation
&#x20;  \- current database models
&#x20;  \- current tests
&#x20;  \- AGENTS.md
&#x20;  \- docs/AGENT\_PROMPTS.md
&#x20;  \- docs/WORK\_LOG.md
3\. Run the current existing test suite and record the baseline result.
4\. Check the current Git branch.
5\. Do not work directly on main.

Create/switch to:

feature/java-springboot-migration

Do not delete, rewrite, or modify the working Python implementation unless absolutely necessary.

\==================================================
JAVA PROJECT FOUNDATION
\==================================================

Create a Maven + Spring Boot project inside the repository.

Use a clear Java package namespace based on CampusSignal.

Before choosing the Java version:
\- inspect the repository/course configuration for any required Java version
\- if none exists, use a current LTS Java version compatible with the selected Spring Boot release
\- document the chosen version and reason

Required dependencies should include only what is currently necessary, such as:

\- Spring Web
\- Spring Data JPA
\- Bean Validation
\- Gmail API client dependencies
\- Google OAuth client dependencies
\- relational database driver suitable for local development/testing
\- Spring Boot Test / JUnit

Do not add AI/LLM dependencies.

Suggested Java structure:

src/main/java/.../campussignal/
&#x20;   CampusSignalApplication.java

&#x20;   config/
&#x20;   controller/
&#x20;   dto/
&#x20;   entity/
&#x20;   repository/
&#x20;   service/
&#x20;   gmail/

src/main/resources/
&#x20;   application.properties
&#x20;   or
&#x20;   application.yml

src/test/java/.../campussignal/

Use normal Spring Boot conventions.

\==================================================
PORT EXISTING EMAIL FUNCTIONALITY
\==================================================

Use the current Python implementation only to understand the required behavior.

Implement Java equivalents for:

1\. Gmail OAuth authorization support
2\. Gmail API client/service
3\. Email retrieval
4\. Email persistence
5\. Duplicate protection

Create an Email entity with at least:

\- id
\- gmailMessageId
\- subject
\- sender
\- receivedAt / timestamp
\- body
\- createdAt

Requirements:

\- gmailMessageId must be unique
\- duplicate Gmail messages must not create duplicate database records
\- database access must use Spring Data JPA
\- Gmail-specific code must remain separated from core persistence/business logic
\- do not hard-code OAuth secrets
\- configuration must use environment variables/properties
\- do not commit credentials, client secrets, tokens, or private keys

If the current Python prototype contains Gmail watch/PubSub/history functionality, do NOT port those components unless they are required for the 11 September milestone.

The required milestone is retrieval, not real-time notification infrastructure.

\==================================================
USER PROFILE
\==================================================

Port the User Profile work into Java.

Create a UserProfile entity containing:

\- id
\- email
\- department
\- programme
\- year
\- semester (optional)
\- interests
\- createdAt

Requirements:

\- email unique and required
\- department required
\- programme required
\- year >= 1
\- semester optional, but if supplied >= 1
\- at least one interest required
\- normalize interests:
&#x20; \- trim whitespace
&#x20; \- lowercase
&#x20; \- remove duplicates while preserving order
\- interests must remain extensible
\- do NOT use a restrictive fixed enum for user interests yet

Use appropriate JPA mapping for interests.

Create request/response DTOs rather than exposing JPA entities directly through controllers.

\==================================================
USER PROFILE API
\==================================================

Implement at minimum:

POST /api/v1/profiles

Creates a user profile.

GET /api/v1/profiles/{id}

Retrieves a profile.

GET /api/v1/profiles/by-email/{email}

Retrieves a profile by email if this is cleanly supported by the architecture.

Use Bean Validation and return appropriate HTTP status codes.

Do not implement authentication/authorization for these profile endpoints unless the current application architecture already provides it.

\==================================================
INITIAL CATEGORY SCORE MODEL
\==================================================

Implement the initial category preference score required by the 11 September milestone.

The proposal defines:

CategoryScore(u, c)
\= 0.40E + 0.15P + 0.35B + 0.10R

where:

E = explicit preference
P = profile relevance
B = behavioural preference
R = related-interest similarity

All inputs must be normalized to [0, 1].

Implementation requirements:

\- create a dedicated CategoryScoreService
\- keep weights configurable
\- do not scatter numeric weights through business logic
\- do not implement final email relevance ranking
\- do not implement semantic embeddings
\- do not implement AI classification

Because this is the cold-start milestone and behavioral data may not yet exist:

\- support B using a neutral/default initial value
\- support R using a simple deterministic initial value or clearly documented placeholder behavior
\- explicit user-selected interests must meaningfully affect E
\- profile information must meaningfully affect P where applicable

Do not pretend that behavioral learning or semantic matching already exists.

The service should be testable independently.

Create a simple response object such as:

{
&#x20; "category": "internships",
&#x20; "score": 0.72,
&#x20; "components": {
&#x20;   "explicitPreference": 1.0,
&#x20;   "profileRelevance": 0.4,
&#x20;   "behavioralPreference": 0.5,
&#x20;   "relatedInterestSimilarity": 0.3
&#x20; }
}

Exact DTO naming may follow project conventions.

\==================================================
CATEGORY SCORE DEMONSTRATION
\==================================================

Add tests showing that two different users can receive different scores for the same category.

Example:

User A interests:
\- internships
\- research
\- hackathons

User B interests:
\- sports
\- cultural events
\- clubs

For category "internships":

score(User A, internships)
must be greater than
score(User B, internships)

This behavior is required for the September milestone.

\==================================================
TESTING
\==================================================

Add Java tests covering at least:

EMAIL:
\- email entity persistence
\- duplicate gmailMessageId protection
\- email retrieval service behavior where practical

PROFILE:
\- valid profile creation
\- invalid empty programme
\- invalid empty department
\- invalid year
\- invalid semester
\- empty interests rejected
\- interest trimming
\- interest lowercase normalization
\- duplicate interest removal
\- repository persistence
\- POST profile endpoint
\- GET profile endpoint

CATEGORY SCORE:
\- score stays in [0,1]
\- explicit selected interest increases the score
\- two users can have different scores for the same category
\- configured weights are used
\- missing behavioral history is handled explicitly and safely

Run the complete Maven test suite.

Do not finish with failing Java tests.

\==================================================
PYTHON CODE
\==================================================

Do NOT delete the Python code during this task.

Create a clear note in documentation that:

\- Python is the earlier prototype/reference implementation
\- Java/Spring Boot is the course-compliant implementation going forward
\- no new product features should be added to Python unless explicitly approved

Do not attempt a line-by-line translation.

Port behavior, not Python architecture.

\==================================================
DOCUMENTATION / TRACEABILITY
\==================================================

Existing documentation rules must continue to be followed.

Update:

docs/AGENT\_PROMPTS.md
docs/WORK\_LOG.md

AGENT\_PROMPTS.md:
Append this entire prompt verbatim.

Do not overwrite previous prompts.

WORK\_LOG.md:
Append a new entry:

\## Work Entry
Date: \<actual date>
Contributor: Priyanshi Garg
Milestone: 11 September 2026 — Email Retrieval and User Preference Setup
Stage: Java/Spring Boot Migration
Branch: \<actual branch>

\### Status
Completed / In Progress / Blocked

\### Reason for migration
Repository prototype was implemented in Python/FastAPI, while the submitted project proposal requires Java, Maven, and Spring Boot.

\### Work completed
\- actual completed work only

\### Python status
\- describe what remains as reference/prototype

\### Java implementation
\- actual modules implemented

\### Files changed
\- actual files only

\### Tests
\- Python baseline result
\- Java focused test results
\- final Maven test result

\### Git
Commit: \<actual hash or Pending>
PR: \<actual PR or Pending>
PR created by: \<actual person or Pending>

\### Next stage
\<actual next stage based on what remains>

Never invent:
\- commit hashes
\- PR numbers
\- PR URLs
\- authors
\- test results

\==================================================
AGENTS.MD
\==================================================

Update AGENTS.md with a short rule stating:

\- The course-compliant backend is Java + Maven + Spring Boot.
\- The existing Python implementation is a prototype/reference only.
\- New backend product functionality must be implemented in Java unless an explicit project decision changes the stack.
\- Every agent must continue maintaining AGENT\_PROMPTS.md and WORK\_LOG.md.

Do not remove existing traceability rules.

\==================================================
DO NOT IMPLEMENT
\==================================================

Do NOT implement:

\- AI email classification
\- LLM integration
\- email summarization
\- deadline extraction
\- eligibility extraction
\- event date extraction
\- embeddings
\- semantic similarity
\- final user-email relevance ranking
\- behavior-learning feedback
\- personalized feed
\- frontend
\- critical-message override

Those belong to later milestones.

\==================================================
SAFETY / SCOPE
\==================================================

Do not:
\- expose credentials
\- commit Google OAuth credentials
\- commit Gmail tokens
\- delete working Python code
\- refactor unrelated files
\- rewrite Git history
\- push directly to main
\- fabricate test results
\- fabricate PR information

If the existing repository architecture creates a serious conflict with this migration, stop and explain the conflict before making a destructive decision.

\==================================================
AT THE END
\==================================================

Report exactly:

1\. branch used
2\. Java version selected and why
3\. Spring Boot version
4\. Maven project structure created
5\. files created
6\. files modified
7\. Gmail functionality ported
8\. email persistence implemented
9\. duplicate handling implemented
10\. user profile functionality implemented
11\. category-score functionality implemented
12\. Python code intentionally left in place
13\. Python baseline test result
14\. Maven/JUnit test result
15\. git diff --stat
16\. git status
17\. whether a commit was created
18\. whether a PR was created
19\. anything still missing for the 11 September milestone

Do not claim the migration is complete unless every required September milestone component listed above is implemented and tested.

---
