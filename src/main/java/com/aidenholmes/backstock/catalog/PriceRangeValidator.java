package com.aidenholmes.backstock.catalog;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

class PriceRangeValidator implements ConstraintValidator<ValidPriceRange, ProductFilterRequest> {

    @Override
    public boolean isValid(ProductFilterRequest value, ConstraintValidatorContext context) {
        BigDecimal minPrice = value.minPrice();
        BigDecimal maxPrice = value.maxPrice();

        if (minPrice == null || maxPrice == null) {
            return true;
        }

        if (maxPrice.compareTo(minPrice) < 0) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("maxPrice")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
