package com.aidenholmes.backstock.catalog;

import com.aidenholmes.backstock.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "categories")
class Category extends AuditableEntity {

    static final int NAME_MAX_LENGTH = 100;
    static final int SLUG_MAX_LENGTH = 100;

    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Column(nullable = false, length = SLUG_MAX_LENGTH)
    private String slug;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    Category(String name, String slug) {
        this.name = name.strip();
        this.slug = slug;
    }

    void rename(String name) {
        this.name = name.strip();
    }

    void deactivate() {
        active = false;
    }
}
