# 0001. Package by feature

## Status

Accepted

## Date

2026-09-26

## Context

Backstock is a single Spring Boot application with several business areas: catalog, users, cart, orders and
security. A typical change touches one area and all of its layers at once: controller, service, repository, entity
and DTOs.

The package layout decides how many packages such a change spans and which classes have to be public.

## Options considered

**Package by layer.** Top-level packages `controller`, `service`, `repository`, `entity`, `dto`. A change to one feature is spread across five packages, and every class must be public to be visible
from the next layer, so nothing inside a feature can be hidden.

**Package by feature.** One package per business area containing all of its layers. A feature is read and changed
in one place, and classes used only inside it can be package-private.

**Hexagonal architecture.** Each feature split into domain, application and adapter packages with ports as
interfaces. Gives strict isolation of the domain, but adds interfaces and mapping layers that a CRUD-heavy
application of this size does not need.

## Decision

We will organize code by feature.

```
com.aidenholmes.backstock
├── catalog/
├── user/
├── cart/
├── order/
├── security/
├── common/
└── config/
```

`common` holds base entity classes, domain exceptions and the global error handler. `config` holds application-wide
configuration. Classes inside a feature package stay flat until the package grows past about ten classes.

## Consequences

- A feature change stays within one package.
- Classes used only inside a feature are package-private.
- Features depend on each other: `cart` and `order` use `catalog`. Classes used across features have to be public,
  so encapsulation is partial.
- Boundaries between features are not checked by tooling and rely on review.
- `common` can turn into a place for everything that does not fit elsewhere.
