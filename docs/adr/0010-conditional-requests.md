# 0010. Conditional requests with entity tags from the version

## Status

Accepted

## Date

2026-10-26

## Context

Categories and products are versioned with `@Version`. The version check covers the time between loading an entity
and flushing it within one transaction.

A client edits a resource in two requests: it reads the resource, then sends an update. Each request runs in its own
transaction, and the update loads the current version. Two clients that read the same version overwrite each other
without an error: the second update loads the version written by the first one and passes the check.

To detect this, the server needs the version the client based its change on. Request DTOs do not contain `version`
(see [conventions](../conventions.md)). JPA does not allow the application to set the version attribute, so a version
received from the client cannot be written into the entity for Hibernate to compare.

Product stock is changed by atomic `UPDATE` statements that do not increment the version, so that stock changes and
edits of product fields do not conflict.

## Options considered

**Version in the request body.** The client sends the version it read, and the service compares it or merges a
detached entity. Request DTOs get a server-managed field, and the mechanism duplicates what HTTP already provides.

**`Last-Modified` with `If-Unmodified-Since`.** Uses `updated_at`. HTTP dates have one-second resolution, so two
changes within one second cannot be told apart.

**Tags computed from the response body.** `ShallowEtagHeaderFilter` hashes the body after the request is handled and
only answers `If-None-Match`; for an update the body already contains the change. `WebRequest.checkNotModified`
evaluates `If-Match`, but needs the current tag in the controller before the change, outside the service transaction.

**Tag from the version with a required `If-Match`.** Responses carry an `ETag` built from the version. Updates send it
back in `If-Match`, and the service compares it with the loaded entity before changing it. The window between the
comparison and the flush is covered by `@Version`.

## Decision

We will build strong entity tags from the entity version and require `If-Match` on updates and deletes in the
catalog.

- `GET /{id}`, `POST` and `PUT` return `ETag` with the version as a strong tag, for example `"3"`.
- `PUT` and `DELETE` require `If-Match` with one strong tag. A request without the header gets 428 Precondition
  Required. `If-Match: *` gets 428 as well: it matches any existing resource and would bypass the check.
- A tag that does not match the current version gets 412 Precondition Failed, and the resource is not changed. A weak
  tag never matches, since `If-Match` uses strong comparison. A list of tags is not supported and gets 412.
- The expected version is passed to the service and compared with the loaded entity before any change. An optimistic
  locking failure on flush in these operations also gets 412: for the client it means the same thing, the resource
  changed after it was read. Optimistic locking failures elsewhere stay 409.
- `If-None-Match` is not handled by the application. Spring answers a matching `If-None-Match` on `GET` with 304 for
  responses that carry an `ETag`, which is correct while everything in the tagged body changes through the version.
- The tag covers only fields that change through the entity. Stock is not part of the tagged product representation:
  it moves to a separate subresource together with the first operation that changes it after creation.

## Consequences

- Lost updates between requests are rejected instead of overwriting another client's changes.
- List responses carry no tags, so a client has to read a resource before updating it.
- Product edits and stock changes do not conflict. Showing a product with its stock takes two requests.
- Any field later changed outside the version has to stay out of a tagged representation, or the tag stops being
  strong.
- Retrying an update after a lost response gets 412 even if the first attempt succeeded. The client has to read the
  resource and compare.
- A weak or malformed tag is rejected before the resource is loaded, so such a request to a missing resource gets 412
  instead of 404.
- Rejecting a list of tags deviates from RFC 9110, which accepts a match with any of them.
- A reverse proxy that compresses responses can turn strong tags into weak ones, after which every update gets 412.
  Compression in front of the API has to keep tags strong or be turned off for it.
