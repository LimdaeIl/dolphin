package com.book.dolphin.product.application.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VariantCreateRequest(
        @NotNull
        Long productId,
        @NotBlank
        String skuCode,
        String size,
        String color,
        String barcode,
        Long weightG,
        Long lengthMm,
        Long widthMm,
        Long heightMm,
        String attributesJson // {"material":"cotton","fit":"regular"} 등
) {

}