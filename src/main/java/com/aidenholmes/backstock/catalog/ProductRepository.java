package com.aidenholmes.backstock.catalog;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);

    boolean existsByCategoryIdAndActiveTrue(Long categoryId);

    Optional<Product> findByIdAndActiveTrue(Long id);
}
