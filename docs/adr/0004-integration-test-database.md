# 0004. Integration test database: PostgreSQL in Testcontainers

## Status

Accepted

## Date

2026-09-26

## Context

Integration tests need a database. The schema is created by Flyway migrations written in PostgreSQL SQL (see
[0003](0003-flyway-migrations.md)) and uses PostgreSQL types and constraints: `numeric`, `timestamptz`, sequences,
`CHECK`. Order processing relies on PostgreSQL locking and transaction behavior under concurrent access.

## Options considered

**H2 in PostgreSQL compatibility mode.** Fast and needs no Docker. The compatibility mode covers only part of the
syntax, so migrations either fail on H2 or need a separate test schema. Locking and isolation differ from PostgreSQL,
so concurrency tests can pass on H2 and fail in production.

**Shared PostgreSQL from docker-compose.** A real database, but tests depend on an environment started by hand and on
data left by previous runs. CI would need its own database service.

**Testcontainers.** Each test run starts a disposable PostgreSQL container, and the application connects to it
through Spring Boot service connections. Requires Docker and adds container startup time.

## Decision

We will run integration tests against PostgreSQL started by Testcontainers. One container is shared by the whole test
run. The image version matches the one in `docker-compose.yml`. H2 is not used.

## Consequences

- Tests run on the same database and the same migrations as the application.
- Test data is not isolated by the shared container and has to be cleaned up between tests.
- Concurrency behavior is tested on the real database engine.
- Running integration tests requires Docker locally and in CI.
- Container startup makes integration tests slower than tests on an in-memory database.
- The PostgreSQL version is defined in two places and has to be updated in both.
