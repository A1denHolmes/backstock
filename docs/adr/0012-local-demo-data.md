# 0012. Local catalog demo data

## Status

Accepted

## Date

2026-11-26

## Context

A local Backstock instance needs sample categories and products so someone can explore the catalog API on a fresh
database. Developers can edit and deactivate those records through the API. Those changes should survive a restart.
A fresh database started with the `test` profile should contain only the data its tests arrange.

[ADR 0003](0003-flyway-migrations.md) uses Flyway for versioned schema changes. The same database should pass Flyway
validation when started with a different profile.

## Options considered

**Local versioned migration.** Flyway applies the seed once and records it in the schema history. A profile that
excludes the script can then see an applied migration it cannot resolve during validation.

**Spring startup code.** A bean scoped to `local` could seed the catalog through the application. This adds Java
startup code and requires coordinating it with schema initialization.

**Local Flyway callback.** A SQL script runs after Flyway has prepared the schema. It can insert missing rows on
each run without adding an entry to the schema history.

## Decision

Use `db/demo/afterMigrate__demo_data.sql` as a Flyway SQL callback. In `application-local.yaml`, set
`spring.flyway.locations` to both `classpath:db/migration` and `classpath:db/demo`. Other profiles use the
standard migration location.

Flyway runs the callback after a successful `migrate()`, including when the schema is already up to date. The
script inserts categories first and resolves each product's foreign key by joining on the slug of an active
category. It skips new products under inactive categories.

Use `ON CONFLICT (slug) DO NOTHING` for categories and `ON CONFLICT (sku) DO NOTHING` for products. The existing
unique keys keep the same meaning after soft deletion, as described in [ADR 0009](0009-soft-delete-in-catalog.md).

## Consequences

- A fresh local database starts with a sample catalog. Repeated runs fill in missing demo rows for active
  categories and preserve existing names, stock levels, and deactivation flags.
- Edits to the seed script affect fresh databases and future inserts. Existing rows keep their values. A fresh
  database provides a clean reset of the demo catalog.
- Switching away from `local` stops future seeding and leaves rows already stored in the database intact. The
  callback has no schema-history entry, so the switch leaves versioned migration validation unaffected.
- `DemoCatalogIT` checks profile isolation, repeated runs, and profile switching against PostgreSQL.
