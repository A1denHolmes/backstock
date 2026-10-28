package com.aidenholmes.backstock.catalog;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);

    boolean existsByCategoryIdAndActiveTrue(Long categoryId);

    Optional<Product> findByIdAndActiveTrue(Long id);

    @Query("SELECT p.stockQuantity FROM Product p WHERE p.id = :id AND p.active = true")
    Optional<Integer> findStockQuantityByIdAndActiveTrue(Long id);

    /**
     * Adds {@code delta} to the stock of an active product if the result stays within the allowed range.
     *
     * @return number of updated rows: 0 if the product is missing or inactive, or the result is out of range
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Product p
            SET p.stockQuantity = p.stockQuantity + :delta
            WHERE p.id = :id AND p.active = true AND p.stockQuantity + :delta BETWEEN 0 AND
        """ + Product.STOCK_QUANTITY_MAX)
    int adjustStockQuantity(Long id, int delta);
}
