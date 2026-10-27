package com.aidenholmes.backstock.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aidenholmes.backstock.common.ConflictException;
import com.aidenholmes.backstock.common.NotFoundException;
import com.aidenholmes.backstock.common.VersionMismatchException;
import com.aidenholmes.backstock.common.Versioned;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-19T10:00:00Z");

    private static final UpdateCategoryRequest RENAME_REQUEST = new UpdateCategoryRequest("Renamed");

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    private CategoryService service;

    @BeforeEach
    void setUp() {
        service = new CategoryService(categoryRepository, productRepository);
    }

    @Test
    void categoryIsCreatedWhenSlugIsFree() {
        when(categoryRepository.existsBySlug("books")).thenReturn(false);
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> persisted(invocation.getArgument(0), 1L, 1L));

        Versioned<CategoryResponse> created = service.create(new CreateCategoryRequest("Books", "books"));

        assertThat(created).isEqualTo(new Versioned<>(new CategoryResponse(1L, "Books", "books", NOW, NOW), 1L));
    }

    @Test
    void categoryWithTakenSlugIsRejected() {
        when(categoryRepository.existsBySlug("books")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateCategoryRequest("Books", "books")))
                .isInstanceOf(ConflictException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void categoryIsFoundById() {
        when(categoryRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(persisted(books(), 1L, 1L)));

        Versioned<CategoryResponse> found = service.findById(1L);

        assertThat(found).isEqualTo(new Versioned<>(new CategoryResponse(1L, "Books", "books", NOW, NOW), 1L));
    }

    @Test
    void unknownCategoryIsNotFound() {
        when(categoryRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void categoriesAreReturnedInRepositoryOrder() {
        when(categoryRepository.findAllByActiveTrueOrderByNameAscIdAsc())
                .thenReturn(List.of(persisted(books(), 1L, 1L), persisted(new Category("Phones", "phones"), 2L, 1L)));

        List<CategoryResponse> found = service.findAll();

        assertThat(found)
                .containsExactly(
                        new CategoryResponse(1L, "Books", "books", NOW, NOW),
                        new CategoryResponse(2L, "Phones", "phones", NOW, NOW));
    }

    @Test
    void existingCategoryIsRenamed() {
        when(categoryRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(persisted(books(), 1L, 1L)));

        CategoryResponse renamed = service.update(1L, RENAME_REQUEST, 1L).body();

        assertThat(renamed).isEqualTo(new CategoryResponse(1L, "Renamed", "books", NOW, NOW));
    }

    @Test
    void unknownCategoryCannotBeRenamed() {
        when(categoryRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(1L, RENAME_REQUEST, 1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void renamingWithOutdatedVersionIsRejected() {
        Category category = persisted(books(), 1L, 2L);
        when(categoryRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.update(1L, RENAME_REQUEST, 1L))
                .isInstanceOf(VersionMismatchException.class)
                .hasMessage("Category 1 was modified by another request. Reload it and try again");
        assertThat(category.getName()).isEqualTo("Books");
        verify(categoryRepository, never()).flush();
    }

    @Test
    void concurrentModificationOnFlushIsReportedAsVersionMismatch() {
        when(categoryRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(persisted(books(), 1L, 1L)));
        doThrow(new OptimisticLockingFailureException("Optimistic locking failure"))
                .when(categoryRepository)
                .flush();

        assertThatThrownBy(() -> service.update(1L, RENAME_REQUEST, 1L))
                .isInstanceOf(VersionMismatchException.class)
                .hasMessage("Category 1 was modified by another request. Reload it and try again")
                .hasCauseInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void categoryWithoutActiveProductsIsDeactivated() {
        Category category = persisted(books(), 1L, 1L);
        when(productRepository.existsByCategoryIdAndActiveTrue(1L)).thenReturn(false);
        when(categoryRepository.findForUpdateByIdAndActiveTrue(1L)).thenReturn(Optional.of(category));

        service.deactivate(1L, 1L);

        assertThat(category.isActive()).isFalse();
    }

    @Test
    void categoryWithActiveProductsCannotBeDeactivated() {
        Category category = persisted(books(), 1L, 1L);
        when(productRepository.existsByCategoryIdAndActiveTrue(1L)).thenReturn(true);
        when(categoryRepository.findForUpdateByIdAndActiveTrue(1L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.deactivate(1L, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Category 1 contains active products");
        assertThat(category.isActive()).isTrue();
    }

    @Test
    void unknownCategoryCannotBeDeactivated() {
        when(categoryRepository.findForUpdateByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(1L, 1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void categoryIsLockedBeforeProductsAreChecked() {
        when(categoryRepository.findForUpdateByIdAndActiveTrue(1L)).thenReturn(Optional.of(persisted(books(), 1L, 1L)));
        when(productRepository.existsByCategoryIdAndActiveTrue(1L)).thenReturn(false);

        service.deactivate(1L, 1L);

        InOrder inOrder = inOrder(categoryRepository, productRepository);
        inOrder.verify(categoryRepository).findForUpdateByIdAndActiveTrue(1L);
        inOrder.verify(productRepository).existsByCategoryIdAndActiveTrue(1L);
    }

    @Test
    void deactivationWithOutdatedVersionIsRejected() {
        Category category = persisted(books(), 1L, 2L);
        when(categoryRepository.findForUpdateByIdAndActiveTrue(1L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.deactivate(1L, 1L))
                .isInstanceOf(VersionMismatchException.class)
                .hasMessage("Category 1 was modified by another request. Reload it and try again");
        assertThat(category.isActive()).isTrue();
        verifyNoInteractions(productRepository);
    }

    private static Category persisted(Category category, long id, long version) {
        ReflectionTestUtils.setField(category, "id", id);
        ReflectionTestUtils.setField(category, "version", version);
        ReflectionTestUtils.setField(category, "createdAt", NOW);
        ReflectionTestUtils.setField(category, "updatedAt", NOW);
        return category;
    }

    private static Category books() {
        return new Category("Books", "books");
    }
}
