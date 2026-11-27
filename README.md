# Backstock

Backstock is a REST API for an online store, built with Java 25, Spring Boot 4
and PostgreSQL 18. The current version implements the product catalog and inventory operations.

## Features

- **Catalog management.** Create, edit and deactivate products and categories.
  A category can be deactivated only after it has no active products.
- **Product browsing.** Browse active products by category and price range.
  Results are paginated and can be sorted by price or creation time.
- **Concurrent edits.** Product and category updates and deletions require `If-Match`.
  A stale `ETag` is rejected, so one client's edit cannot silently overwrite another's.
- **Stock adjustments.** Increase or decrease a product's stock with a single atomic
  database update. An adjustment that would put the quantity outside the allowed range is rejected.

## Run locally

You'll need Git, JDK 25, Docker with Compose, and Bash.
On Windows, run these commands in WSL with Docker integration enabled.

```bash
git clone https://github.com/A1denHolmes/backstock.git
cd backstock
cp .env.example .env
docker compose up -d --wait postgres

set -a && source .env && set +a
./mvnw spring-boot:run
```

The `local` profile seeds a fresh database with 3 categories and 10 products.
Open [Swagger UI](http://localhost:8080/swagger-ui.html) to explore the API,
or try this request in a second terminal:

```bash
curl 'http://localhost:8080/api/v1/products?minPrice=40&maxPrice=50&sort=price,asc'
```

On a fresh database, the results include *Effective Java*, priced at 49.90.

## Verification

With Docker running:

```bash
./mvnw verify
```

The build runs unit tests and integration tests against PostgreSQL 18 using Testcontainers.
It also checks formatting, code style and the JaCoCo coverage threshold. GitHub Actions runs
the same Maven phase on pull requests.

## Documentation

- [Architecture decision records](docs/adr)
- [Coding conventions](docs/conventions.md)
- [OpenAPI specification](http://localhost:8080/v3/api-docs) when running locally.
  Swagger UI and the OpenAPI endpoint are disabled in the `prod` profile.

## License

Distributed under the [MIT License](LICENSE).
