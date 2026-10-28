package com.aidenholmes.backstock.catalog;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);

    boolean existsByCategoryIdAndActiveTrue(Long categoryId);

    Optional<Product> findByIdAndActiveTrue(Long id);

    @Query("SELECT p.stockQuantity FROM Product p WHERE p.id = :id AND p.active = true")
    Optional<Integer> findStockQuantityByIdAndActiveTrue(Long id);
}
