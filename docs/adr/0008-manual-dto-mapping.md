# 0008. Manual mapping between entities and DTOs

## Status

Accepted

## Date

2026-10-20

## Context

Services return DTOs, and entities are not exposed outside the service layer. With `open-in-view: false` the
persistence context closes when the service method returns, so mapping has to happen inside the transaction.

Responses are records built from entities. Requests are records whose components are passed to entity constructors
and domain methods. Entities have no setters, and nullable persisted fields such as `id` have to be checked when
mapped (see [0007](0007-null-safety.md)).

## Options considered

**MapStruct.** An annotation processor generates mapper implementations at compile time. Mappings are type-checked,
and unmapped properties can fail the build. It adds a third annotation processor next to Lombok and Error Prone, and
the generated code in our packages falls under NullAway checks. Mapping into entities relies on setters or
constructors and cannot call domain methods.

**ModelMapper.** Matches properties by name through reflection at runtime. No generated code. Mismatches are found
only when the mapping runs, and name-based matching can silently map the wrong property.

**Manual mapping.** Each response record has a static `from()` method. Services pass request components to entity
constructors and domain methods. No dependency and no generated code.

## Decision

We will write mapping code by hand.

A response record maps an entity in a static `from()` method. A request record is not mapped into an entity: the
service reads its components and calls the entity constructor or a domain method. No mapper classes or mapping
libraries are used.

## Consequences

- Mapping is plain Java, checked by the compiler and NullAway, and debugged like any other code.
- Every response record repeats its fields in `from()`.
- Adding a component to a response record breaks compilation of its `from()` method until it is updated.
- A new entity field is not exposed until it is added to a response record.
- `from()` may read lazy associations, so it is called inside the service transaction.
