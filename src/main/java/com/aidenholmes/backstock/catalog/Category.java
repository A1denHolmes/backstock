package com.aidenholmes.backstock.catalog;

import com.aidenholmes.backstock.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "categories")
class Category extends AuditableEntity {

    static final int NAME_MAX_LENGTH = 100;
    static final String NAME_PATTERN = "\\P{C}*";
    static final String NAME_PATTERN_MESSAGE = "must not contain control or invisible formatting characters";
    static final int SLUG_MAX_LENGTH = 100;
    static final String SLUG_PATTERN = "[a-z0-9]+(?:-[a-z0-9]+)*";
    static final String SLUG_PATTERN_MESSAGE =
            "must contain lowercase Latin letters and digits, separated by single hyphens";

    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Column(nullable = false, length = SLUG_MAX_LENGTH)
    private String slug;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Version
    private long version;

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
