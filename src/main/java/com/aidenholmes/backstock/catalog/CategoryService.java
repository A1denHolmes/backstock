package com.aidenholmes.backstock.catalog;

import com.aidenholmes.backstock.common.ConflictException;
import com.aidenholmes.backstock.common.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
    CategoryResponse findById(long id) {
        return CategoryResponse.from(findActiveOrThrow(id));
    }

    @Transactional
    CategoryResponse create(CreateCategoryRequest request) {
        if (categoryRepository.existsBySlug(request.slug())) {
            throw new ConflictException("Category with slug \"" + request.slug() + "\" already exists");
        }

        Category created = categoryRepository.save(new Category(request.name(), request.slug()));
        return CategoryResponse.from(created);
    }

    @Transactional
    CategoryResponse update(long id, UpdateCategoryRequest request) {
        Category category = findActiveOrThrow(id);

        category.rename(request.name());
        categoryRepository.flush();

        return CategoryResponse.from(category);
    }

    @Transactional
    void deactivate(long id) {
        Category category = findActiveOrThrow(id);
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
}
