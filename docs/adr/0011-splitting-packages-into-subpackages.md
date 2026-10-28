# 0011. Splitting packages into subpackages

## Status

Accepted

## Date

2026-10-28

## Context

[ADR 0001](0001-package-by-feature.md) keeps a feature package flat until it grows past about ten classes. `catalog`
now has 17 classes and `common` has 11, and finding a class or its test in the project tree takes longer.

A subpackage in Java is an independent package: a package-private class in `catalog` is not visible from
`catalog.dto`. Every class in `catalog` is package-private. Categories and products depend on each other: `Product`
references `Category`, and each service uses the other's repository.

Classes in `common` are used by features, so all of them except the exception handler are already public.

## Options considered

**Split a feature by layer.** Subpackages such as `web`, `service`, `persistence` and `dto`. Almost every class has to
become public to be visible from the next layer, which is the problem package by feature was chosen to avoid.

**Split a feature by aggregate.** `catalog.category` and `catalog.product`. The two subpackages depend on each other,
and `Category` and both repositories have to become public.

**Move only DTOs to a subpackage.** The static `from()` of a response reads the entity, so entities and all DTOs have
to become public.

**Split by what a subpackage exposes.** A group of classes moves to a subpackage only when the rest of the package uses
a small part of it. The number of classes alone is not a reason.

## Decision

We will split a package into subpackages when a group of its classes exposes one or two types to the rest of the
package, not when the package reaches a certain size. This replaces the size rule in ADR 0001.

- `catalog` stays flat.
- `common` is split by responsibility, since its classes are public anyway:
    - `common.persistence`: `BaseEntity`, `AuditableEntity`;
    - `common.error`: domain exceptions and `GlobalExceptionHandler`;
    - `common.web`: `EntityTags`, `Versioned`, `PreconditionRequiredException`. It belongs here rather than in
      `common.error` because it reports a missing HTTP header, not a broken domain rule.
- Test packages mirror main packages. Test support classes stay in `common`.

## Consequences

- Classes in `catalog` stay package-private, and a change to the catalog stays within one package.
- Feature packages can grow long. Classes in them are found by name rather than by browsing the tree.
- A shared class has to fit one of the `common` subpackages. A class that fits none of them probably belongs to a
  feature.
- Features import shared classes from the subpackages of `common`.
- `@NullMarked` does not apply to subpackages, so every new subpackage needs its own `package-info.java`.
- A type in a subpackage that the parent package uses has to be public and is visible to every feature. Keeping it
  inside its feature relies on review, as with other feature boundaries.
