# CampusSignal Contributor Guide

## Purpose

CampusSignal is an intelligent university information-routing system. Its intended flow is raw campus messages through ingestion, classification, structured extraction, validation, storage, student matching, and finally personalised feeds and reminders.

## Architecture

The course-compliant backend uses Java + Maven + Spring Boot under `src/main/java/edu/campussignal`, with controllers, DTOs, entities, Spring Data repositories, services, configuration, and a separate Gmail adapter. Java tests live under `src/test/java`.

The earlier Python/FastAPI prototype remains as a behavioral reference:

- `app/api`: HTTP routes and request dependencies.
- `app/schemas`: Pydantic API and canonical data contracts.
- `app/models`: SQLAlchemy persistence models.
- `app/repositories`: database access operations.
- `app/services`: business logic and source adapters.
- `app/core` and `app/db`: configuration and database infrastructure.
- `tests`: automated behaviour and persistence tests.

Keep source adapters behind the `EmailSource` contract. Business ingestion must remain independent of Gmail, HTTP, and other provider-specific details.

## Current milestone

The 11 September 2026 milestone covers Java Gmail OAuth and retrieval, relational email storage with duplicate protection, user profiles and their API, and initial category preference scores. Python watch/Pub/Sub/history remains reference-only. AI/NLP, embeddings, final email ranking, behavioral learning, feeds, frontend, and deployment infrastructure remain out of scope.

## Backend stack rules

- The course-compliant backend is Java + Maven + Spring Boot.
- The existing Python implementation is a prototype/reference only.
- New backend product functionality must be implemented in Java unless an explicit project decision changes the stack.
- Every agent must continue maintaining AGENT_PROMPTS.md and WORK_LOG.md.

## Coding conventions

- Target Java 25; follow normal Spring Boot conventions and keep controllers thin.
- Keep routes thin; put business decisions in services and persistence operations in repositories.
- Use validated request/response DTOs at Java HTTP boundaries and Spring Data JPA with database constraints for integrity.
- Python reference maintenance retains Python 3.11+, type hints, Pydantic schemas, and SQLAlchemy 2 conventions.
- Prefer small, readable modules and descriptive names.
- Add or update tests for every behaviour change.

## Working rules for future Codex tasks

- Make small, testable changes and do not expand the requested milestone implicitly.
- Never expose secrets in output, logs, source code, fixtures, or documentation.
- Never commit credentials or local `.env` files.
- Run the relevant tests after every change and report the command and result.
- Preserve duplicate protection and source independence when extending ingestion.

## Project Traceability Rules

- Every coding-agent task must append the exact task prompt to docs/AGENT_PROMPTS.md before or during implementation.
- Every completed development task must append an entry to docs/WORK_LOG.md.
- Never overwrite previous entries.
- Never invent commit hashes, PR numbers, PR URLs, authors, dates, or test results.
- If a PR has not yet been created, record its status as "Pending".
- Documentation updates are part of the definition of done.
