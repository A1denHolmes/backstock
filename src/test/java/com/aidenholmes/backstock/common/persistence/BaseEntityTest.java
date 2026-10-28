package com.aidenholmes.backstock.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class BaseEntityTest {

    static class TestEntity extends BaseEntity {}

    static class OtherTestEntity extends BaseEntity {}

    @Test
    void entitiesOfSameTypeWithSameIdAreEqual() {
        TestEntity first = withId(new TestEntity(), 1L);
        TestEntity second = withId(new TestEntity(), 1L);

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
    }

    @Test
    void entitiesOfSameTypeWithDifferentIdsAreNotEqual() {
        TestEntity first = withId(new TestEntity(), 1L);
        TestEntity second = withId(new TestEntity(), 2L);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void entitiesOfDifferentTypesWithSameIdAreNotEqual() {
        BaseEntity first = withId(new TestEntity(), 1L);
        BaseEntity second = withId(new OtherTestEntity(), 1L);

        assertThat(first).isNotEqualTo(second);
        assertThat(second).isNotEqualTo(first);
    }

    @Test
    void entitiesWithoutIdAreNotEqual() {
        assertThat(new TestEntity()).isNotEqualTo(new TestEntity());
    }

    @Test
    void entityWithoutIdIsEqualToItself() {
        TestEntity entity = new TestEntity();

        assertThat(entity.equals(entity)).isTrue();
    }

    @Test
    void entityIsNotEqualToNull() {
        TestEntity entity = new TestEntity();

        assertThat(entity.equals(null)).isFalse();
    }

    @Test
    void hashCodeDoesNotChangeWhenIdIsAssigned() {
        TestEntity entity = new TestEntity();
        int hashBeforeId = entity.hashCode();

        withId(entity, 1L);

        assertThat(entity.hashCode()).isEqualTo(hashBeforeId);
    }

    private static <T extends BaseEntity> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
