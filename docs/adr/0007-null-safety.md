# 0007. Null safety with JSpecify and NullAway

## Status

Accepted

## Date

2026-10-14

## Context

Spring Framework 7 declares the nullability of its API with JSpecify annotations: packages are `@NullMarked`, so
types are non-null by default, and values that may be null are marked `@Nullable`. Code without annotations has no
such contract. The IDE warns when it overrides an annotated Spring method, and a missing null check is found only
when it fails at runtime.

Annotating only some packages gives inconsistent contracts, so the decision covers the whole code base. Little code
exists yet, and adding annotations later costs more.

Entities do not follow a constructor-initialized model. Hibernate creates them through a no-argument constructor and
sets fields by reflection, and `id` and the audit timestamps stay null until the entity is persisted.

## Options considered

**No annotations.** Nothing to maintain. Nullability is known only from documentation and from reading the code.

**JSpecify annotations checked by the IDE.** Contracts are visible in signatures and match Spring. Violations show
up as IDE warnings that the build does not enforce.

**JSpecify annotations checked by NullAway.** NullAway runs as an Error Prone plugin during compilation and fails the
build on a violation. It is fast and needs annotations only where null is allowed. It is not sound: it assumes that
unannotated libraries return non-null values and that a method without arguments returns the same value when called
twice. Error Prone depends on internal javac APIs.

**Checker Framework Nullness Checker.** Sound for the code it checks, including field initialization. Requires more
annotations, such as `@MonotonicNonNull` and initialization qualifiers, and slows compilation noticeably.

## Decision

We will annotate main code with JSpecify and check it with NullAway.

Every package has a `package-info.java` marked `@NullMarked`. Values that may be null are marked `@Nullable`, written
before the type.

NullAway is the only enabled Error Prone check, and its findings are errors. It treats all of
`com.aidenholmes.backstock` as annotated, so a package without `package-info.java` is still checked. Test code is not
checked. The flags that open javac internals to Error Prone are kept in `.mvn/jvm.config`.

Entity fields set when the entity is persisted, such as `id`, `createdAt` and `updatedAt`, are `@Nullable`. Fields
set by constructors are non-null. NullAway does not check field initialization in the no-argument constructor of
JPA-managed classes, so entities need no suppressions.

A request DTO component without `@Nullable` has a Bean Validation constraint that rejects null. Jackson sets
components by reflection, so NullAway cannot check them, and validation rejects null before the controller is called.

## Consequences

- Nullability is part of method signatures and matches the Spring API.
- Dereferencing a nullable value without a check fails the build.
- Values set by reflection, such as entity fields loaded by Hibernate and request DTO components set by Jackson, are
  not checked by NullAway.
- Values returned by unannotated libraries are assumed to be non-null.
- Mapping a persisted entity to a DTO has to handle the nullable `id` explicitly.
- The build depends on internal javac APIs. A JDK upgrade may require a newer Error Prone version.
- Test code can use null freely. The IDE may still warn about it.
- `@NullMarked` does not apply to subpackages, so a new package needs its own `package-info.java`. Without it,
  NullAway still checks the package, but the IDE does not.
