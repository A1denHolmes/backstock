package com.aidenholmes.backstock.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aidenholmes.backstock.common.AbstractIntegrationTest;
import com.aidenholmes.backstock.common.ConflictException;
import com.aidenholmes.backstock.common.VersionMismatchException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

class ProductServiceIT extends AbstractIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductService service;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void concurrentDeactivationMakesUpdateFail() {
        Category books = categoryRepository.save(newBooks());
        Category phones = categoryRepository.save(new Category("Phones", "phones"));
        long id = Objects.requireNonNull(productRepository.save(newBook(books)).getId());

        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        TransactionTemplate inner = new TransactionTemplate(transactionManager);
        inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> outer.executeWithoutResult(outerStatus -> {
                    Product product =
                            productRepository.findByIdAndActiveTrue(id).orElseThrow();
                    inner.executeWithoutResult(innerStatus -> service.deactivate(id, product.getVersion()));
                    product.update(
                            "Phone", "Description", new BigDecimal("20.00"), phones, "https://example.com/a.jpg");
                }))
                .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(productRepository.findByIdAndActiveTrue(id)).isEmpty();
    }

    @Test
    void updateAfterConcurrentUpdateFailsWithVersionMismatch() {
        Category category = categoryRepository.save(newBooks());
        long categoryId = Objects.requireNonNull(category.getId());
        long productId =
                Objects.requireNonNull(productRepository.save(newBook(category)).getId());

        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        TransactionTemplate inner = new TransactionTemplate(transactionManager);
        inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> outer.executeWithoutResult(outerStatus -> {
                    Product product =
                            productRepository.findByIdAndActiveTrue(productId).orElseThrow();
                    long readVersion = product.getVersion();
                    inner.executeWithoutResult(innerStatus ->
                            service.update(productId, updateRequest("Renamed", categoryId), readVersion));
                    service.update(productId, updateRequest("RenamedTwice", categoryId), readVersion);
                }))
                .isInstanceOf(VersionMismatchException.class)
                .hasMessage("Product " + productId + " was modified by another request. Reload it and try again")
                .hasCauseInstanceOf(OptimisticLockingFailureException.class);
        assertThat(productRepository
                        .findByIdAndActiveTrue(productId)
                        .orElseThrow()
                        .getName())
                .isEqualTo("Renamed");
    }

    @Test
    void updateReturnsIncrementedVersion() {
        Category category = categoryRepository.save(newBooks());
        long categoryId = Objects.requireNonNull(category.getId());

        Product product = productRepository.save(newBook(category));
        long productId = Objects.requireNonNull(product.getId());
        long version = product.getVersion();

        long newVersion = service.update(productId, updateRequest("Renamed", categoryId), version)
                .version();

        assertThat(newVersion).isEqualTo(version + 1);
    }

    @Test
    void deactivationAfterConcurrentUpdateFailsWithVersionMismatch() {
        Category category = categoryRepository.save(newBooks());
        long categoryId = Objects.requireNonNull(category.getId());
        long productId =
                Objects.requireNonNull(productRepository.save(newBook(category)).getId());

        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        TransactionTemplate inner = new TransactionTemplate(transactionManager);
        inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> outer.executeWithoutResult(outerStatus -> {
                    Product product =
                            productRepository.findByIdAndActiveTrue(productId).orElseThrow();
                    long readVersion = product.getVersion();
                    inner.executeWithoutResult(innerStatus ->
                            service.update(productId, updateRequest("Renamed", categoryId), readVersion));
                    service.deactivate(productId, readVersion);
                }))
                .isInstanceOf(VersionMismatchException.class)
                .hasMessage("Product " + productId + " was modified by another request. Reload it and try again")
                .hasCauseInstanceOf(OptimisticLockingFailureException.class);
        assertThat(productRepository.findByIdAndActiveTrue(productId)).isPresent();
    }

    @Test
    @Timeout(10)
    void concurrentDecreasesSucceedOnlyWhileStockLasts() throws Exception {
        int initialStock = 5;
        int delta = -1;
        int threads = 10;

        Category category = categoryRepository.save(newBooks());
        long id = Objects.requireNonNull(productRepository
                .save(new Product("Book", "BOOK", null, new BigDecimal("10.00"), initialStock, category, null))
                .getId());

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch ready = new CountDownLatch(threads);
        List<Future<Boolean>> results = new ArrayList<>(threads);
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            for (int i = 0; i < threads; i++) {
                results.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        service.adjustStock(id, new AdjustStockRequest(delta));
                        return true;
                    } catch (ConflictException e) {
                        return false;
                    }
                }));
            }
            ready.await();
            start.countDown();
        }

        int successes = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                successes++;
            }
        }

        int stock = service.findStock(id).quantity();

        assertThat(successes).isEqualTo(initialStock);
        assertThat(stock).isEqualTo(initialStock + successes * delta);
    }

    private static Category newBooks() {
        return new Category("Books", "books");
    }

    private static Product newBook(Category category) {
        return new Product("Book", "BOOK", null, new BigDecimal("10.00"), 1, category, null);
    }

    private static UpdateProductRequest updateRequest(String name, long categoryId) {
        return new UpdateProductRequest(name, null, new BigDecimal("20.00"), categoryId, null);
    }
}
