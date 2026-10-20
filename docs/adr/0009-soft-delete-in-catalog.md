# 0009. Soft delete in the catalog

## Status

Accepted

## Date

2026-10-20

## Context

Categories and products are removed from the catalog through the API, but their rows are still needed. Products
reference categories through a foreign key, and orders will reference products. A row that is referenced cannot be
deleted without breaking or rewriting the references.

Category slugs and product SKUs identify resources in URLs and outside the application. Some queries have to see
removed rows, such as the uniqueness check for a slug.

## Options considered

**Hard delete.** The simplest model. Deleting a referenced row fails on the foreign key or requires cascading the
delete into order history. A deleted slug or SKU becomes free and can be taken by another resource.

**Global filter in Hibernate.** `@SQLDelete` with `@SQLRestriction`, or the native `@SoftDelete`, turn `delete()`
into an update and add the condition to every query of the entity, so it cannot be forgotten. Removed rows are
hidden from every query, including the ones that need them. `@SQLDelete` replaces the SQL of the delete and bypasses
the version check unless the version is written into it. `@SoftDelete` is incubating.

**Flag column with explicit query conditions.** An `is_active` column is set to `false` by a domain method. Each
repository query states whether it includes inactive rows. Deactivation is an ordinary entity change. Every new query
has to remember the condition.

## Decision

We will soft delete categories and products with an `is_active` column and explicit conditions in queries.

`DELETE` calls a domain method that sets `is_active` to `false`. Queries that serve API reads select only active
rows. Queries that check uniqueness or resolve references include inactive rows. No global Hibernate filters are
used.

Deactivation cannot be reverted through the API. A product that runs out of stock is not deactivated; it stays in
the catalog with zero stock. Slugs of deactivated categories and SKUs of deactivated products are not reused, and
unique constraints cover all rows.

## Consequences

- Referenced rows are never deleted, so foreign keys do not block removal and order history stays intact.
- A read query without the active condition exposes removed rows. Each read query is covered by a test that
  excludes inactive rows.
- A plain unique constraint is enough because values are never reused. Reuse would require a partial unique index.
- Deactivation goes through dirty checking and is protected by optimistic locking.
- Deactivated rows accumulate and are not purged.
- A mistaken deactivation can only be reverted in the database.
