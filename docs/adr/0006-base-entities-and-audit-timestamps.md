# 0006. Base entity classes and audit timestamps

## Status

Accepted

## Date

2026-10-11

## Context

All entities share an identifier (see [0002](0002-bigint-sequence-identifiers.md)) and need the same definition of
equality. Entities are placed in sets and maps, compared across persistence contexts and reach equality checks as
Hibernate proxies. A definition repeated in every entity drifts, and a wrong one breaks collections without failing
loudly.

Most entities also record when they were created and last modified. Setting these values in services means every
write path has to remember to do it.

## Options considered

### Timestamps

**Database defaults and triggers.** Correct for any write, including raw SQL. The values exist only in the database,
so the entity in memory does not see them until it is reloaded, and part of the behavior lives outside the
application code.

**JPA lifecycle callbacks.** Methods annotated with `@PrePersist` and `@PreUpdate` in a base class set the values
directly. Plain Jakarta Persistence with no extra dependency. The time comes from a direct call to `Instant.now()`
and cannot be replaced in tests.

**Hibernate `@CreationTimestamp` and `@UpdateTimestamp`.** Declarative, and can take the time from the database.
Ties the mapping to Hibernate-specific annotations.

**Spring Data JPA auditing.** `@CreatedDate` and `@LastModifiedDate` are filled by `AuditingEntityListener` through
JPA callbacks. Spring Data JPA is already a dependency. The time comes from a replaceable `DateTimeProvider` bean,
and the same mechanism supports `@CreatedBy` and `@LastModifiedBy`. Auditing has to be enabled explicitly.

### Equality

**Object identity.** The default `equals` and `hashCode`. The same row loaded in two persistence contexts gives two
unequal objects.

**Identifier with `instanceof`.** In a shared base class it makes entities of different types with the same id equal.

**Identifier with exact class comparison.** Entities are equal when their persistent classes match and their ids are
equal and non-null. Hibernate proxies have to be resolved to the entity class.

## Decision

We will use two mapped superclasses in `common`.

`BaseEntity` holds the identifier and defines equality. Two entities are equal when they have the same persistent
class and the same non-null id. An entity without an id is equal only to itself. Proxies are resolved through
`HibernateProxy` without being initialized; `Hibernate.getClass()` is not used because it initializes the proxy.
`hashCode` returns the hash of the persistent class, so it does not change when the id is assigned. Both methods are
final and read the id through its getter.

`AuditableEntity` extends `BaseEntity` and adds `createdAt` and `updatedAt` of type `Instant`, filled by Spring Data
JPA auditing. Auditing is enabled in a separate configuration class, not on the application class. Entities without
timestamps extend `BaseEntity` directly.

## Consequences

- Equality is defined once and cannot be overridden by entities.
- Entities of different types are never equal, even with the same id.
- `BaseEntity` depends on the Hibernate proxy API.
- All entities of one type share a hash code, so large hash-based collections of one type lose constant-time lookup.
- Timestamps come from the application clock and are set on persist and on flush of a modified entity.
- Bulk JPQL updates and native SQL bypass the listener and have to set `updated_at` themselves.
- Test slices do not load the auditing configuration. `@DataJpaTest` imports it explicitly.
