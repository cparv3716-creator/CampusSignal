## Work Entry
Date: 2026-09-08
Contributor: Priyanshi Garg
Milestone: 11 September 2026 — Email Retrieval and User Preference Setup
Stage: User Profile Foundation
Branch: feature/user-profile

### Status
Completed

### Work completed
- Inspected the existing architecture, schemas, models, database initialization, and test fixtures; confirmed a clean checkout already on feature/user-profile.
- Added UserProfile using the existing SQLAlchemy Base, with a unique required email, required department/programme/year, nullable semester, JSON interests, and an automatic UTC creation timestamp.
- Registered UserProfile through app.models; the existing init_db imports that package and creates the table without changes to database initialization or Gmail code.
- Added UserProfileCreate and UserProfileRead with email validation, nonempty department/programme, positive year/semester, required string interests, whitespace trimming, lowercase normalization, ordered deduplication, blank-interest rejection, extra-field rejection, ORM reading, and UTC timestamp normalization.
- Added focused schema, registration, constraint, and persistence tests using existing database/client fixtures; verified registration independently in a fresh interpreter.
- Recorded the complete task prompt and added persistent Project Traceability Rules to AGENTS.md.

### Files changed
- app/models/user_profile.py (created)
- app/models/__init__.py (modified)
- app/schemas/user_profile.py (created)
- tests/test_user_profile.py (created)
- docs/AGENT_PROMPTS.md (created)
- docs/WORK_LOG.md (created)
- AGENTS.md (modified)

### Tests
- Baseline: python -m pytest — 35 passed, 1 warning in 0.46s, before implementation.
- New focused tests: python -m pytest tests/test_user_profile.py -v — 42 passed, 1 warning in 0.64s.
- Final complete suite: python -m pytest — 77 passed, 1 warning in 1.05s.
- Environment: the initial global-Python attempt could not run because pytest was absent. Two virtual-environment attempts each reported 20 passed and 15 setup errors because sandbox permissions blocked pytest temporary directories. All successful runs used the existing .venv Python on PATH, approved execution outside the sandbox, and PYTEST_ADDOPTS specifying a fresh UUID-suffixed --basetemp directory under .pytest_cache. No application changes were needed to resolve these environment errors.
- The single warning in each successful run is the existing Starlette/AnyIO BlockingPortal deprecation warning.

### Git
Commit: Pending
PR: Pending
PR created by: Pending

### Next stage
User Profile API/service persistence, followed by Initial Category Preference Score implementation.

---
