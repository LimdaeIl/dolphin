package com.book.dolphin.product.presentation;

import com.book.dolphin.common.response.ApiResponse;
import com.book.dolphin.product.application.dto.request.InitInventoryRequest;
import com.book.dolphin.product.application.dto.request.QuantityRequest;
import com.book.dolphin.product.application.dto.response.InventoryResponse;
import com.book.dolphin.product.application.service.InventoryService;
import jakarta.validation.Valid;
import java.net.URI;
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
@RequestMapping("/api/v1/inventories")
@RestController
public class InventoryController {

    private final InventoryService inventoryService;

    // 1) 재고 초기화(상품 생성 이후 1회)
    @PostMapping
    public ResponseEntity<ApiResponse<InventoryResponse>> init(
            @Valid @RequestBody InitInventoryRequest request
    ) {
        InventoryResponse response = inventoryService.init(
                request.skuCode(),
                request.productId(),
                request.onHand(),
                request.safetyStock(),
                request.backorderable()
        );
        return ResponseEntity
                .created(URI.create("/api/v1/inventories/" + response.id()))
                .body(ApiResponse.success(response));
    }

    // 2) 입고(+)
    @PostMapping("/{inventoryId}/inbound")
    public ResponseEntity<ApiResponse<InventoryResponse>> inbound(
            @PathVariable Long inventoryId,
            @Valid @RequestBody QuantityRequest req
    ) {
        InventoryResponse response = inventoryService.inbound(inventoryId, req.quantity(),
                req.reason());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 3) 할당(장바구니/주문확정 전)
    @PostMapping("/{inventoryId}/allocate")
    public ResponseEntity<ApiResponse<InventoryResponse>> allocate(
            @PathVariable Long inventoryId,
            @Valid @RequestBody QuantityRequest req
    ) {
        InventoryResponse response = inventoryService.allocate(inventoryId, req.quantity(),
                req.reason());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 4) 할당 해제(주문 취소/만료)
    @PostMapping("/{inventoryId}/deallocate")
    public ResponseEntity<ApiResponse<InventoryResponse>> deallocate(
            @PathVariable Long inventoryId,
            @Valid @RequestBody QuantityRequest req
    ) {
        InventoryResponse response = inventoryService.deallocate(inventoryId, req.quantity(),
                req.reason());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 5) 출고(결제 성공)
    @PostMapping("/{inventoryId}/ship")
    public ResponseEntity<ApiResponse<InventoryResponse>> ship(
            @PathVariable Long inventoryId,
            @Valid @RequestBody QuantityRequest req
    ) {
        InventoryResponse response = inventoryService.ship(inventoryId, req.quantity(),
                req.reason());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 0) 단건 조회
    @GetMapping("/{inventoryId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getOne(@PathVariable Long inventoryId) {
        InventoryResponse response = inventoryService.getById(inventoryId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // (선택) 조건 조회: productId, skuCode 중 하나 이상
    @GetMapping
    public ResponseEntity<ApiResponse<InventoryResponse>> getByKey(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String skuCode
    ) {
        InventoryResponse response = inventoryService.getByKey(productId, skuCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
