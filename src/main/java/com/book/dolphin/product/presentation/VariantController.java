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

    /**
     * P0
     * GET /{id} (단건 조회): 장바구니/결제에 정확한 규격 노출.
     * GET /by-product?productId=: 상품 상세에서 옵션 스와치(사이즈/색상) 렌더링용.
     *
     * P1
     * PATCH /{id}: 사이즈/색상/바코드/치수/attributesJson 변경.
     * 중복 SKU 재검증: productId + skuCode 유니크 유지(변경 시 충돌 방지).
     * Bulk 생성/수정: 여러 옵션을 한 번에 등록/갱신.
     *
     * P2
     * 옵션 조합 가용성 API: 색상 선택 시 가능한 사이즈만 반환 등 UX 개선.
     * SEO/표시명 규칙: displayName(예: “블랙 / M”) 관리.
     */
}
