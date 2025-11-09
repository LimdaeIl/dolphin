package com.book.dolphin.product.application.service;

import com.book.dolphin.product.application.dto.request.VariantCreateRequest;
import com.book.dolphin.product.application.dto.response.VariantResponse;
import com.book.dolphin.product.domain.entity.Product;
import com.book.dolphin.product.domain.entity.ProductVariant;
import com.book.dolphin.product.domain.exception.ProductErrorCode;
import com.book.dolphin.product.domain.exception.ProductException;
import com.book.dolphin.product.domain.repository.ProductRepository;
import com.book.dolphin.product.domain.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class VariantService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;


    @Transactional
    public VariantResponse create(VariantCreateRequest request) {
        if (request.skuCode() == null || request.skuCode().isBlank()) {
            throw new ProductException(ProductErrorCode.BLANK_SKU_CODE);
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ProductException(
                        ProductErrorCode.NOT_FOUND_PRODUCT, request.productId()));

        boolean exists = productVariantRepository
                .existsByProductIdAndSkuCode(request.productId(), request.skuCode());
        if (exists) {
            throw new ProductException(ProductErrorCode.DUPLICATE_VARIANT_SKU, request.skuCode());
        }

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .skuCode(request.skuCode())
                .size(request.size())
                .color(request.color())
                .barcode(request.barcode())
                .weightG(request.weightG())
                .lengthMm(request.lengthMm())
                .widthMm(request.widthMm())
                .heightMm(request.heightMm())
                .attributesJson(request.attributesJson())
                .build();
        try {
            ProductVariant saved = productVariantRepository.save(variant);
            return VariantResponse.of(saved);
        } catch (
                DataIntegrityViolationException ex) {
            throw new ProductException(ProductErrorCode.DUPLICATE_VARIANT_SKU, request.skuCode());
        }
    }
}
