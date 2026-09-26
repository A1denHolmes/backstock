# 0003. Schema migrations with Flyway

## Status

Accepted

## Date

2026-09-26

## Context

The database schema has to be reproducible on any machine, in tests and in CI, and every change to it has to be
tracked together with the code that depends on it.

The schema uses PostgreSQL features that JPA mappings cannot express, such as `CHECK` constraints for enum columns.

## Options considered

**Hibernate schema generation (`ddl-auto: update` or `create`).** No extra tooling. The schema is derived from
entities, so constraints beyond the mapping are lost. There is no history of changes, `update` never drops or renames
anything, and the result depends on the current state of the database.

**Liquibase.** Versioned changelogs in XML, YAML or SQL with database-agnostic change types and rollbacks. The
abstraction over databases is not needed with a single PostgreSQL database.

**Flyway.** Versioned plain SQL migrations applied in order at startup. The applied history is stored in the database.

## Decision

We will manage the schema with Flyway from the first table. Migrations are plain SQL in `src/main/resources/db/migration`,
named `V<n>__<description>.sql`.

Hibernate does not change the schema. It runs with `ddl-auto: validate` and checks the mappings against the migrated
schema at startup.

## Consequences

- The schema is created the same way everywhere: locally, in integration tests and in CI.
- A migration that has reached `main` is never edited. A change is a new migration.
- An entity change and its migration go into the same commit.
- A mismatch between entities and tables fails startup instead of surfacing at runtime.
- Validation covers tables, columns and types. Indexes and constraints are not checked at startup.
