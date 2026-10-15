# CampusSignal Contributor Guide

## Purpose

CampusSignal is an intelligent university information-routing system. It ingests campus email, classifies and stores it, extracts actionable metadata, matches it to student profiles, and presents deterministic personalized action feeds.

## Active architecture

CampusSignal is a Java 25, Maven, and Spring Boot application.

- Production code: `src/main/java/edu/campussignal`
- Application configuration: `src/main/resources`
- Automated Java tests: `src/test/java`
- Test configuration: `src/test/resources`
- Static-analysis configuration: `config`
- Build definition and wrapper: `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/`

Use Spring MVC controllers and validated DTOs at HTTP boundaries, Spring Data JPA entities and repositories for persistence, and H2 for local and test storage. Gmail OAuth, Gmail API, watch/history synchronization, and Pub/Sub adapters are implemented in Java. Deterministic services provide classification, confidence, action extraction, category relevance, urgency, and feed ranking.

Keep provider adapters behind application contracts such as `EmailSource`. Business ingestion must remain independent of Gmail, HTTP, and other provider-specific details.

## Coding conventions

- Target Java 25 and follow normal Spring Boot conventions.
- Keep controllers thin; put decisions in services and persistence operations in repositories.
- Use validated request/response DTOs and never expose JPA entities directly from controllers.
- Enforce integrity with validation, transactions, repository operations, and database constraints.
- Keep deterministic classification and ranking explainable and stable.
- Prefer small, readable modules and descriptive names.
- Add or update JUnit tests for every behavior change.

## Safety and scope

- Make small, testable changes and do not expand the requested milestone implicitly.
- Preserve email duplicate protection and source independence when extending ingestion.
- Do not weaken, skip, disable, or delete Java tests to obtain a passing build.
- Preserve Gmail OAuth, retrieval, watch/history synchronization, H2 persistence, classification, backfill, profiles, category scores, action tracking, urgency, and ranked-feed behavior unless a task explicitly changes it.
- Never expose secrets in output, logs, source, fixtures, or documentation.
- Never commit credentials, OAuth tokens, private keys, local `.env` files, or local databases.
- Run relevant Maven tests after every change and report the exact command and result.

## Project traceability

- Every coding-agent task must append the exact task prompt to `docs/AGENT_PROMPTS.md` before or during implementation.
- Every completed development task must append an entry to `docs/WORK_LOG.md`.
- Never overwrite previous entries.
- Never invent commit hashes, PR numbers, PR URLs, authors, dates, or test results.
- If a PR has not been created, record its status as `Pending`.
- Documentation updates are part of the definition of done.
