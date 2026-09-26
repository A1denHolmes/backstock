# 0005. MVP scope and deliberate exclusions

## Status

Accepted

## Date

2026-09-26

## Context

Backstock is developed by one person. An online store can grow in many directions: payments, delivery, promotions,
search, media, integrations. Each feature adds code that has to be designed, tested and maintained, and a wide
feature set built quickly tends to stay shallow.

The core of the domain is placing an order correctly when many users buy the same product at the same time.

## Options considered

**Broad feature set.** Cover most of what a real store has, including payments, delivery and promotions. Shows
breadth, but each part gets less attention, and the hard parts compete for time with routine CRUD.

**Narrow core at production quality.** Implement only what an order needs end to end and bring it to production
standards: validation, security, concurrency, tests, documentation. Everything else is excluded explicitly.

## Decision

We will build a narrow core:

- product catalog with categories, pagination and API documentation
- user registration and JWT authentication with refresh token rotation, roles and rate limiting
- shopping cart
- order placement that stays correct under concurrent access and is idempotent
- running the whole system with a single command

The application runs as a single instance with one PostgreSQL database and a single currency.

Excluded:

- **Infrastructure concerns.** TLS termination and certificates (handled by a reverse proxy), mTLS, database
  encryption, CORS, a separate Actuator management port, Kubernetes, microservices.
- **Scaling.** Redis cache, message brokers, distributed rate limiting.
- **Domain features.** Multiple currencies, product images and object storage, nested categories, warehouses, stock
  adjustment history, reviews and ratings, promo codes, payment, delivery.
- **Other.** Storing response codes and bodies for idempotent requests, bot protection beyond a per-account purchase
  limit, a changelog.

## Consequences

- Time goes into correctness of the order flow rather than into the number of features.
- Rate limit counters are kept in memory. They are correct for one instance; with several instances limits become
  per instance and need a shared store.
