package com.aidenholmes.backstock.catalog;

import com.aidenholmes.backstock.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "products")
class Product extends AuditableEntity {

    private static final int NAME_MAX_LENGTH = 200;
    private static final int SKU_MAX_LENGTH = 64;
    private static final int IMAGE_URL_MAX_LENGTH = 2048;

    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Column(nullable = false, length = SKU_MAX_LENGTH)
    private String sku;

    private @Nullable String description;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(nullable = false, updatable = false)
    private int stockQuantity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(length = IMAGE_URL_MAX_LENGTH)
    private @Nullable String imageUrl;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Version
    private long version;

    Product(
            String name,
            String sku,
            @Nullable String description,
            BigDecimal price,
            int stockQuantity,
            Category category,
            @Nullable String imageUrl) {
        this.sku = sku;
        this.name = name.strip();
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.category = category;
        this.imageUrl = imageUrl;
    }
}
