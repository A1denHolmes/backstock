package com.aidenholmes.backstock.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aidenholmes.backstock.common.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.util.Objects;
import org.junit.jupiter.api.Test;
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
        Category books = categoryRepository.save(new Category("Books", "books"));
        Category phones = categoryRepository.save(new Category("Phones", "phones"));
        long id = Objects.requireNonNull(productRepository
                .save(new Product("Book", "BOOK", null, new BigDecimal("10.00"), 1, books, null))
                .getId());

        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        TransactionTemplate inner = new TransactionTemplate(transactionManager);
        inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> outer.executeWithoutResult(outerStatus -> {
                    Product product =
                            productRepository.findByIdAndActiveTrue(id).orElseThrow();
                    inner.executeWithoutResult(innerStatus -> service.deactivate(id));
                    product.update(
                            "Phone", "Description", new BigDecimal("20.00"), phones, "https://example.com/a.jpg");
                }))
                .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(productRepository.findByIdAndActiveTrue(id)).isEmpty();
    }
}
