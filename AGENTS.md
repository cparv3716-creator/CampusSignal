# CampusSignal Contributor Guide

## Purpose

CampusSignal is an intelligent university information-routing system. Its intended flow is raw campus messages through ingestion, classification, structured extraction, validation, storage, student matching, and finally personalised feeds and reminders.

## Architecture

The backend uses a layered FastAPI architecture:

- `app/api`: HTTP routes and request dependencies.
- `app/schemas`: Pydantic API and canonical data contracts.
- `app/models`: SQLAlchemy persistence models.
- `app/repositories`: database access operations.
- `app/services`: business logic and source adapters.
- `app/core` and `app/db`: configuration and database infrastructure.
- `tests`: automated behaviour and persistence tests.

Keep source adapters behind the `EmailSource` contract. Business ingestion must remain independent of Gmail, HTTP, and other provider-specific details.

## Current milestone

The current milestone includes the backend foundation, local/manual raw-email ingestion, one-shot Gmail ingestion, and near-real-time local Gmail monitoring through `users.watch`, Cloud Pub/Sub pull delivery, and Gmail history synchronization. Public webhooks, deployment infrastructure, AI or LLM processing, frontend work, and recommendation logic remain out of scope until explicitly requested.

## Coding conventions

- Target Python 3.11 or newer and use type hints on public functions and important local boundaries.
- Keep routes thin; put business decisions in services and persistence operations in repositories.
- Use Pydantic schemas at external and service boundaries.
- Use SQLAlchemy 2-style APIs and database constraints for integrity.
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
