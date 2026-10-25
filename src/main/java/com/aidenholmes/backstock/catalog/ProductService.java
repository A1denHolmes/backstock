package com.aidenholmes.backstock.catalog;

import com.aidenholmes.backstock.common.ConflictException;
import com.aidenholmes.backstock.common.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ProductService {

    private final ProductRepository productRepository;

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    ProductResponse findById(long id) {
        return ProductResponse.from(findActiveOrThrow(id));
    }

    @Transactional
    ProductResponse create(CreateProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new ConflictException("Product with SKU \"" + request.sku() + "\" already exists");
        }

        Category category = findActiveCategoryOrThrow(request.categoryId());
        Product created = productRepository.save(new Product(
                request.name(),
                request.sku(),
                request.description(),
                request.price(),
                request.stockQuantity(),
                category,
                request.imageUrl()));

        return ProductResponse.from(created);
    }

    @Transactional
    ProductResponse update(long id, UpdateProductRequest request) {
        Product product = findActiveOrThrow(id);
        Category category = findActiveCategoryOrThrow(request.categoryId());

        product.update(request.name(), request.description(), request.price(), category, request.imageUrl());
        productRepository.flush();

        return ProductResponse.from(product);
    }

    @Transactional
    void deactivate(long id) {
        findActiveOrThrow(id).deactivate();
    }

    private Product findActiveOrThrow(long id) {
        return productRepository
                .findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Product " + id + " not found"));
    }

    private Category findActiveCategoryOrThrow(long id) {
        return categoryRepository
                .findForShareByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Category " + id + " not found"));
    }
}
