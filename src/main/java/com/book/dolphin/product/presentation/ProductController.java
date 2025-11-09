package com.book.dolphin.product.presentation;

import com.book.dolphin.common.response.ApiResponse;
import com.book.dolphin.product.application.dto.request.ProductCreateRequest;
import com.book.dolphin.product.application.dto.response.ProductListPage;
import com.book.dolphin.product.application.dto.response.ProductResponse;
import com.book.dolphin.product.application.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
@RestController
public class ProductController {

    private final ProductService productService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @Valid @RequestBody ProductCreateRequest request
    ) {
        ProductResponse response = productService.create(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * TODO 리스트
     * P0 (먼저)
     * GET /{id} (단건 조회): 생성 직후/상세 화면 진입용. 카테고리·가격(현재가 계산)·대표/본문 이미지 포함.
     * GET /list (목록 + 필터/정렬/페이지네이션): keyword, categoryId, status, priceMin/Max, sort(최신/가격/인기) 등. 쇼핑 리스트/검색의 기반.
     * POST /{id}/publish, /{id}/archive: 공개/보관 전환. 공개 시 카테고리 ACTIVE 검증 재사용.
     *
     * P1 (다음)
     * PATCH /{id} (이름/본문 수정): 마크다운/HTML 본문 교체, rename, changeContent 호출.
     * PUT /{id}/categories: 대표 1개 제한/중복 방지 검증 포함한 재매핑(일괄 교체).
     * PUT /{id}/prices: 정가/할인가 설정(기간 검증, 중복 할인 방지).
     * PUT /{id}/media: 대표/본문 이미지 일괄 정렬/추가/삭제(최대 8장 규칙 체크).
     *
     * P2 (이후)
     * DELETE /{id} (소프트 삭제): 운영 데이터 보존.
     * 추천/연관 상품 API: 카테고리/유사 특성 기반.
     * 캐시 및 ETag: 제품 상세/리스트 캐싱, 304 응답.
     *
     */


    // 1) 상세
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getOne(@PathVariable Long id) {
        ProductResponse response = productService.getOne(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 2) 목록/검색/정렬/페이지
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<ProductListPage>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,       // e.g. DRAFT/PUBLISHED/ARCHIVED
            @RequestParam(required = false) String sort,         // PRICE_ASC / PRICE_DESC / RECENT(default)
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size
    ) {
        ProductListPage pageDto = productService.list(keyword, categoryId, status, sort, page, size);
        return ResponseEntity.ok(ApiResponse.success(pageDto));
    }

    // 3) 상태 전환
    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<ProductResponse>> publish(@PathVariable Long id) {
        ProductResponse response = productService.publish(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<ProductResponse>> archive(@PathVariable Long id) {
        ProductResponse response = productService.archive(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
