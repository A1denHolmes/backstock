package com.aidenholmes.backstock.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aidenholmes.backstock.common.ConflictException;
import com.aidenholmes.backstock.common.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-19T10:00:00Z");

    @Mock
    private CategoryRepository repository;

    private CategoryService service;

    @BeforeEach
    void setUp() {
        service = new CategoryService(repository);
    }

    @Test
    void categoryIsCreatedWhenSlugIsFree() {
        when(repository.existsBySlug("books")).thenReturn(false);
        when(repository.save(any(Category.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0), 1L));

        CategoryResponse created = service.create(new CreateCategoryRequest("Books", "books"));

        assertThat(created).isEqualTo(new CategoryResponse(1L, "Books", "books", NOW, NOW));
    }

    @Test
    void categoryWithTakenSlugIsRejected() {
        when(repository.existsBySlug("books")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateCategoryRequest("Books", "books")))
                .isInstanceOf(ConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void categoryIsFoundById() {
        when(repository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(persisted(new Category("Books", "books"), 1L)));

        CategoryResponse found = service.findById(1L);

        assertThat(found).isEqualTo(new CategoryResponse(1L, "Books", "books", NOW, NOW));
    }

    @Test
    void unknownCategoryIsNotFound() {
        when(repository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void categoriesAreReturnedInRepositoryOrder() {
        when(repository.findAllByActiveTrueOrderByNameAscIdAsc())
                .thenReturn(List.of(
                        persisted(new Category("Books", "books"), 1L),
                        persisted(new Category("Phones", "phones"), 2L)));

        List<CategoryResponse> found = service.findAll();

        assertThat(found)
                .containsExactly(
                        new CategoryResponse(1L, "Books", "books", NOW, NOW),
                        new CategoryResponse(2L, "Phones", "phones", NOW, NOW));
    }

    @Test
    void existingCategoryIsRenamed() {
        when(repository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(persisted(new Category("Books", "books"), 1L)));

        CategoryResponse renamed = service.update(1L, new UpdateCategoryRequest("Renamed"));

        assertThat(renamed).isEqualTo(new CategoryResponse(1L, "Renamed", "books", NOW, NOW));
    }

    @Test
    void unknownCategoryCannotBeRenamed() {
        when(repository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(1L, new UpdateCategoryRequest("Renamed")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void activeCategoryIsDeactivated() {
        Category category = persisted(new Category("Books", "books"), 1L);
        when(repository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(category));

        service.deactivate(1L);

        assertThat(category.isActive()).isFalse();
    }

    @Test
    void unknownCategoryCannotBeDeactivated() {
        when(repository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(1L)).isInstanceOf(NotFoundException.class);
    }

    private static Category persisted(Category category, long id) {
        ReflectionTestUtils.setField(category, "id", id);
        ReflectionTestUtils.setField(category, "createdAt", NOW);
        ReflectionTestUtils.setField(category, "updatedAt", NOW);
        return category;
    }
}
