package com.aidenholmes.backstock.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aidenholmes.backstock.common.AbstractIntegrationTest;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

class CategoryServiceIT extends AbstractIntegrationTest {

    @Autowired
    private CategoryRepository repository;

    @Autowired
    private CategoryService service;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void concurrentDeactivationMakesRenameFail() {
        long id = Objects.requireNonNull(
                repository.save(new Category("Books", "books")).getId());
        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        TransactionTemplate inner = new TransactionTemplate(transactionManager);
        inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> outer.executeWithoutResult(outerStatus -> {
                    Category category = repository.findByIdAndActiveTrue(id).orElseThrow();
                    inner.executeWithoutResult(innerStatus -> service.deactivate(id));
                    category.rename("Renamed");
                }))
                .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(repository.findByIdAndActiveTrue(id)).isEmpty();
    }

    @Test
    void updateResponseContainsNewModificationTimestamp() {
        CategoryResponse created = service.create(new CreateCategoryRequest("Books", "books"));

        CategoryResponse updated = service.update(created.id(), new UpdateCategoryRequest("Renamed"));

        assertThat(updated.updatedAt()).isAfter(created.updatedAt());
    }
}
