package com.aidenholmes.backstock.catalog;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(value = RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PriceRangeValidator.class)
@interface ValidPriceRange {

    String message() default "must be greater than or equal to minPrice";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
