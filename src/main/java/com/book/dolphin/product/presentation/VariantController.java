package com.book.dolphin.product.presentation;

import com.book.dolphin.common.response.ApiResponse;
import com.book.dolphin.product.application.dto.request.VariantCreateRequest;
import com.book.dolphin.product.application.dto.response.VariantResponse;
import com.book.dolphin.product.application.service.VariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/v1/variants")
@RestController
public class VariantController {

    private final VariantService variantService;

    @PostMapping
    public ResponseEntity<ApiResponse<VariantResponse>> createVariant(
            @Valid @RequestBody VariantCreateRequest request) {
        VariantResponse response = variantService.create(request); // product 조회 + (productId, skuCode) 유니크 검증
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
