# Conventions

Formatting follows [.editorconfig](../.editorconfig) and is checked by Spotless and Checkstyle.

## Code

- Packages are split by feature: `catalog`, `user`, `cart`, `order`, `security`. Shared code goes to `common`,
  configuration to `config`.
- Dependencies are injected through the constructor.
- Transactions are declared on services. Read-only methods use `@Transactional(readOnly = true)`.
- Services return DTOs. Entities are not exposed outside the service layer.
- DTOs are records named `...Request` and `...Response`. Mapping is written by hand in a static `from()`.
- Request DTOs do not contain server-managed fields: `id`, `role`, `version`, status and audit fields.
- Endpoints that take a resource ID check that the caller owns the resource.
- Endpoints are versioned in the path: `/api/v1/...`. Each controller is documented with springdoc annotations.
- Logging uses SLF4J. Personal data and credentials are not logged.

## Data

| What    | Java                           | PostgreSQL                         |
|---------|--------------------------------|------------------------------------|
| Money   | `BigDecimal`, output scale 2   | `numeric(19,4)`                    |
| Time    | `Instant`, UTC                 | `timestamptz`                      |
| Enums   | `@Enumerated(EnumType.STRING)` | `varchar` with `CHECK`             |
| Strings | `@Column(length = n)`          | `varchar(n)`, `text` for free text |

Tables are named in plural, entities in singular. Schema changes go through Flyway migrations.

## Tests

- Unit tests end with `Test`, integration tests with `IT`.
- Integration tests run against PostgreSQL in Testcontainers.
- Instruction coverage must stay at 60% or higher.

## Commits

[Conventional Commits](https://www.conventionalcommits.org/): `<type>(<scope>): <subject>`, subject in imperative
mood, up to 72 characters.

Types: `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `build`, `chore`.

Scope is the feature package, or one of `build`, `ci`, `docker`, `deps`, `adr`, `config`. Repository-wide changes
have no scope.

## Pull requests

- One pull request per change.
- CI must pass. Pull requests are squash-merged.
- A significant design decision comes with an ADR in [adr/](adr). Accepted ADRs are not edited; a new ADR
  supersedes an old one.
- New environment variables are added to `.env.example`.
