package com.aidenholmes.backstock.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Base class for entities that record when they were created and updated.
 *
 * <p>The timestamps are set by {@link AuditingEntityListener} when the entity is persisted and when its changes are
 * flushed.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class AuditableEntity extends BaseEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private @Nullable Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private @Nullable Instant updatedAt;
}
