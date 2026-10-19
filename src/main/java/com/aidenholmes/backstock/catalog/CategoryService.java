package com.aidenholmes.backstock.catalog;

import com.aidenholmes.backstock.common.ConflictException;
import com.aidenholmes.backstock.common.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CategoryService {

    private final CategoryRepository repository;

    CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    List<CategoryResponse> findAll() {
        return repository.findAllByActiveTrueOrderByNameAscIdAsc().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    CategoryResponse findById(long id) {
        return CategoryResponse.from(findActiveOrThrow(id));
    }

    @Transactional
    CategoryResponse create(CreateCategoryRequest request) {
        if (repository.existsBySlug(request.slug())) {
            throw new ConflictException("Category with slug \"" + request.slug() + "\" already exists");
        }

        Category created = repository.save(new Category(request.name(), request.slug()));
        return CategoryResponse.from(created);
    }

    @Transactional
    CategoryResponse update(long id, UpdateCategoryRequest request) {
        Category category = findActiveOrThrow(id);

        category.rename(request.name());
        repository.flush();

        return CategoryResponse.from(category);
    }

    @Transactional
    void deactivate(long id) {
        findActiveOrThrow(id).deactivate();
    }

    private Category findActiveOrThrow(long id) {
        return repository
                .findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Category " + id + " not found"));
    }
}
