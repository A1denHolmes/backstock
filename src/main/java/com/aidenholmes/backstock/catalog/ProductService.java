package com.aidenholmes.backstock.catalog;

import com.aidenholmes.backstock.common.ConflictException;
import com.aidenholmes.backstock.common.NotFoundException;
import com.aidenholmes.backstock.common.VersionMismatchException;
import com.aidenholmes.backstock.common.Versioned;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ProductService {

    private final ProductRepository productRepository;

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    Versioned<ProductResponse> findById(long id) {
        Product product = findActiveOrThrow(id);

        return new Versioned<>(ProductResponse.from(product), product.getVersion());
    }

    @Transactional
    Versioned<ProductResponse> create(CreateProductRequest request) {
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

        return new Versioned<>(ProductResponse.from(created), created.getVersion());
    }

    @Transactional
    Versioned<ProductResponse> update(long id, UpdateProductRequest request, long expectedVersion) {
        Product product = findActiveOrThrow(id);

        if (product.getVersion() != expectedVersion) {
            throw new VersionMismatchException(versionMismatchDetail(id));
        }

        Category category = findActiveCategoryOrThrow(request.categoryId());

        product.update(request.name(), request.description(), request.price(), category, request.imageUrl());
        try {
            productRepository.flush();
        } catch (OptimisticLockingFailureException e) {
            throw new VersionMismatchException(versionMismatchDetail(id), e);
        }

        return new Versioned<>(ProductResponse.from(product), product.getVersion());
    }

    @Transactional
    void deactivate(long id, long expectedVersion) {
        Product product = findActiveOrThrow(id);

        if (product.getVersion() != expectedVersion) {
            throw new VersionMismatchException(versionMismatchDetail(id));
        }

        product.deactivate();
        try {
            productRepository.flush();
        } catch (OptimisticLockingFailureException e) {
            throw new VersionMismatchException(versionMismatchDetail(id), e);
        }
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

    private static String versionMismatchDetail(long id) {
        return "Product " + id + " was modified by another request. Reload it and try again";
    }
}
