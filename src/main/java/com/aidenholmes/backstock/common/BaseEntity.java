package com.aidenholmes.backstock.common;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.proxy.HibernateProxy;
import org.jspecify.annotations.Nullable;

/**
 * Base class for entities with a sequence-generated id.
 *
 * <p>Two entities are equal when they have the same persistent class and the same non-null id.
 * Entities of different types are never equal. An entity without an id is equal only to itself.
 * The hash code depends on the class, so it does not change when the id is assigned on persist.
 */
@MappedSuperclass
public abstract class BaseEntity {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private @Nullable Long id;

    @Override
    public final boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || effectiveClass(this) != effectiveClass(obj)) {
            return false;
        }
        BaseEntity other = (BaseEntity) obj;
        // This method is final and runs on the proxy itself, whose fields are never populated,
        // so the id is read through the getter
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public final int hashCode() {
        return effectiveClass(this).hashCode();
    }

    // Resolves a proxy to its entity class without initializing it, unlike Hibernate.getClass()
    private static Class<?> effectiveClass(Object o) {
        return o instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
    }
}
