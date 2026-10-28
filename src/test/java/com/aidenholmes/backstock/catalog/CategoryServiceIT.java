package com.aidenholmes.backstock.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aidenholmes.backstock.common.AbstractIntegrationTest;
import com.aidenholmes.backstock.common.error.ConflictException;
import com.aidenholmes.backstock.common.error.NotFoundException;
import com.aidenholmes.backstock.common.error.VersionMismatchException;
import com.aidenholmes.backstock.common.web.Versioned;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

class CategoryServiceIT extends AbstractIntegrationTest {

    private static final UpdateCategoryRequest RENAME_REQUEST = new UpdateCategoryRequest("Renamed");

    @Autowired
    private CategoryRepository repository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void concurrentDeactivationMakesRenameFail() {
        long id = Objects.requireNonNull(repository.save(newBooks()).getId());
        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        TransactionTemplate inner = new TransactionTemplate(transactionManager);
        inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> outer.executeWithoutResult(outerStatus -> {
                    Category category = repository.findByIdAndActiveTrue(id).orElseThrow();
                    inner.executeWithoutResult(innerStatus -> categoryService.deactivate(id, category.getVersion()));
                    category.rename("Renamed");
                }))
                .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(repository.findByIdAndActiveTrue(id)).isEmpty();
    }

    @Test
    void renameAfterConcurrentRenameFailsWithVersionMismatch() {
        long id = Objects.requireNonNull(repository.save(newBooks()).getId());
        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        TransactionTemplate inner = new TransactionTemplate(transactionManager);
        inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> outer.executeWithoutResult(outerStatus -> {
                    Category category = repository.findByIdAndActiveTrue(id).orElseThrow();
                    long readVersion = category.getVersion();
                    inner.executeWithoutResult(innerStatus -> categoryService.update(id, RENAME_REQUEST, readVersion));
                    categoryService.update(id, new UpdateCategoryRequest("RenamedTwice"), readVersion);
                }))
                .isInstanceOf(VersionMismatchException.class)
                .hasMessage("Category " + id + " was modified by another request. Reload it and try again")
                .hasCauseInstanceOf(OptimisticLockingFailureException.class);
        assertThat(repository.findByIdAndActiveTrue(id).orElseThrow().getName()).isEqualTo("Renamed");
    }

    @Test
    void updateReturnsIncrementedVersion() {
        Category category = repository.save(newBooks());
        long id = Objects.requireNonNull(category.getId());
        long version = category.getVersion();

        long newVersion = categoryService.update(id, RENAME_REQUEST, version).version();

        assertThat(newVersion).isEqualTo(version + 1);
    }

    @Test
    void updateResponseContainsNewModificationTimestamp() {
        Versioned<CategoryResponse> created = categoryService.create(new CreateCategoryRequest("Books", "books"));

        CategoryResponse updated = categoryService
                .update(created.body().id(), RENAME_REQUEST, created.version())
                .body();

        assertThat(updated.updatedAt()).isAfter(created.body().updatedAt());
    }

    @Test
    void deactivationWaitsForConcurrentProductCreationAndFails() throws Exception {
        Category category = repository.save(newBooks());
        long categoryId = Objects.requireNonNull(category.getId());
        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch commitAllowed = new CountDownLatch(1);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> creation = executor.submit(() -> transaction.executeWithoutResult(status -> {
                productService.create(
                        new CreateProductRequest("Book", "BOOK", null, new BigDecimal("10.00"), 1, categoryId, null));
                lockAcquired.countDown();
                awaitOrFail(commitAllowed);
            }));
            assertThat(lockAcquired.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> deactivation =
                    executor.submit(() -> categoryService.deactivate(categoryId, category.getVersion()));
            assertThatThrownBy(() -> deactivation.get(500, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            commitAllowed.countDown();

            creation.get(5, TimeUnit.SECONDS);
            assertThatThrownBy(() -> deactivation.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(ConflictException.class);
        }
    }

    @Test
    void productCreationWaitsForConcurrentDeactivationAndFails() throws Exception {
        Category category = repository.save(newBooks());
        long categoryId = Objects.requireNonNull(category.getId());
        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch commitAllowed = new CountDownLatch(1);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> deactivation = executor.submit(() -> transaction.executeWithoutResult(status -> {
                categoryService.deactivate(categoryId, category.getVersion());
                lockAcquired.countDown();
                awaitOrFail(commitAllowed);
            }));
            assertThat(lockAcquired.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> creation = executor.submit(() -> productService.create(
                    new CreateProductRequest("Book", "BOOK", null, new BigDecimal("10.00"), 1, categoryId, null)));
            assertThatThrownBy(() -> creation.get(500, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            commitAllowed.countDown();

            deactivation.get(5, TimeUnit.SECONDS);
            assertThatThrownBy(() -> creation.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(NotFoundException.class);
        }
    }

    private static void awaitOrFail(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Latch was not released in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static Category newBooks() {
        return new Category("Books", "books");
    }
}
