package com.aidenholmes.backstock.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Optional<Category> findByIdAndActiveTrue(Long id);

    List<Category> findAllByActiveTrueOrderByNameAscIdAsc();
}
