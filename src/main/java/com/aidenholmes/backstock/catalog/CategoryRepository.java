package com.aidenholmes.backstock.catalog;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Optional<Category> findByIdAndActiveTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Category> findForUpdateByIdAndActiveTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<Category> findForShareByIdAndActiveTrue(Long id);

    List<Category> findAllByActiveTrueOrderByNameAscIdAsc();
}
