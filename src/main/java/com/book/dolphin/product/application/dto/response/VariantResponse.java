package com.book.dolphin.product.application.dto.response;

import com.book.dolphin.product.domain.entity.ProductVariant;

public record VariantResponse(
        Long id,
        Long productId,
        String skuCode,
        String size,
        String color,
        String barcode,
        Long weightG,
        Long lengthMm,
        Long widthMm,
        Long heightMm,
        String attributesJson
) {
    public static VariantResponse of(ProductVariant v) {
        return new VariantResponse(
                v.getId(),
                v.getProduct().getId(),
                v.getSkuCode(),
                v.getSize(),
                v.getColor(),
                v.getBarcode(),
                v.getWeightG(),
                v.getLengthMm(),
                v.getWidthMm(),
                v.getHeightMm(),
                v.getAttributesJson()
        );
    }
}