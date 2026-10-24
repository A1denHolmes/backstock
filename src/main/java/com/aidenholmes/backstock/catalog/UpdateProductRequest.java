package com.aidenholmes.backstock.catalog;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.hibernate.validator.constraints.URL;
import org.jspecify.annotations.Nullable;

record UpdateProductRequest(
        @NotBlank
        @Size(min = 1, max = Product.NAME_MAX_LENGTH)
        @Pattern(regexp = Product.NAME_PATTERN, message = Product.NAME_PATTERN_MESSAGE)
        @Schema(example = "Phone")
        String name,

        @Size(min = 1, max = Product.DESCRIPTION_MAX_LENGTH) @Schema(example = "New product's description.") @Nullable
        String description,

        @NotNull @Positive @Digits(integer = 15, fraction = 2) @Schema(example = "200.00")
        BigDecimal price,

        @NotNull Long categoryId,

        @Size(min = 1, max = Product.IMAGE_URL_MAX_LENGTH)
        @URL(protocol = "https")
        @Schema(example = "https://example.com/newpicture.jpg")
        @Nullable
        String imageUrl) {}
