package com.aidenholmes.backstock.catalog;

import com.aidenholmes.backstock.common.ConflictException;
import com.aidenholmes.backstock.common.NotFoundException;
import com.aidenholmes.backstock.common.VersionMismatchException;
import com.aidenholmes.backstock.common.Versioned;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CategoryService {

    private final CategoryRepository categoryRepository;

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    List<CategoryResponse> findAll() {
        return categoryRepository.findAllByActiveTrueOrderByNameAscIdAsc().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    Versioned<CategoryResponse> findById(long id) {
        Category found = findActiveOrThrow(id);

        return new Versioned<>(CategoryResponse.from(found), found.getVersion());
    }

    @Transactional
    Versioned<CategoryResponse> create(CreateCategoryRequest request) {
        if (categoryRepository.existsBySlug(request.slug())) {
            throw new ConflictException("Category with slug \"" + request.slug() + "\" already exists");
        }

        Category created = categoryRepository.save(new Category(request.name(), request.slug()));
        return new Versioned<>(CategoryResponse.from(created), created.getVersion());
    }

    @Transactional
    Versioned<CategoryResponse> update(long id, UpdateCategoryRequest request, long expectedVersion) {
        Category category = findActiveOrThrow(id);

        if (category.getVersion() != expectedVersion) {
            throw new VersionMismatchException(versionMismatchDetail(id));
        }

        category.rename(request.name());
        try {
            categoryRepository.flush();
        } catch (OptimisticLockingFailureException e) {
            throw new VersionMismatchException(versionMismatchDetail(id), e);
        }

        return new Versioned<>(CategoryResponse.from(category), category.getVersion());
    }

    @Transactional
    void deactivate(long id, long expectedVersion) {
        Category category = categoryRepository
                .findForUpdateByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Category " + id + " not found"));

        if (category.getVersion() != expectedVersion) {
            throw new VersionMismatchException(versionMismatchDetail(id));
        }

        if (productRepository.existsByCategoryIdAndActiveTrue(id)) {
            throw new ConflictException("Category " + id + " contains active products");
        }

        category.deactivate();
    }

    private Category findActiveOrThrow(long id) {
        return categoryRepository
                .findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Category " + id + " not found"));
    }

    private static String versionMismatchDetail(long id) {
        return "Category " + id + " was modified by another request. Reload it and try again";
    }
}
