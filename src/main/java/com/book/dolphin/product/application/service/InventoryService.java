package com.book.dolphin.product.application.service;

import com.book.dolphin.product.domain.entity.Inventory;
import com.book.dolphin.product.domain.entity.InventoryLedger;
import com.book.dolphin.product.domain.entity.InventoryLedger.LedgerEventType;
import com.book.dolphin.product.domain.entity.Product;
import com.book.dolphin.product.domain.exception.ProductErrorCode;
import com.book.dolphin.product.domain.exception.ProductException;
import com.book.dolphin.product.domain.repository.InventoryLedgerRepository;
import com.book.dolphin.product.domain.repository.InventoryRepository;
import com.book.dolphin.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryLedgerRepository ledgerRepository;
    private final ProductRepository productRepository;

    @Transactional
    public Inventory init(String skuCode, Long productId, long onHand, long safetyStock,
            boolean backorderable) {
        Product product = productRepository.findById(productId)
                .orElseThrow(
                        () -> new ProductException(ProductErrorCode.NOT_FOUND_PRODUCT, productId));

        // (선택) 사전 중복 가드 — uk_inventory_product_sku 위반을 사전에 차단
        boolean exists = inventoryRepository.existsByProductIdAndSkuCode(productId, skuCode);
        if (exists) {
            throw new ProductException(ProductErrorCode.ALREADY_EXISTS_INVENTORY, productId,
                    skuCode);
        }

        Inventory inv = Inventory.of(product, skuCode, onHand, safetyStock, backorderable);
        return inventoryRepository.save(inv);
    }

    @Transactional
    public Inventory inbound(Long inventoryId, long qty, String reason) {
        Inventory inv = get(inventoryId);
        inv.increaseOnHand(qty);
        ledgerRepository.save(InventoryLedger.builder()
                .inventory(inv).eventType(LedgerEventType.INBOUND).quantity(+qty).reason(reason)
                .build());
        return inv;
    }

    @Transactional
    public Inventory allocate(Long inventoryId, long qty, String reason) {
        Inventory inv = get(inventoryId);
        inv.allocate(qty);
        ledgerRepository.save(InventoryLedger.builder()
                .inventory(inv).eventType(LedgerEventType.ALLOCATE).quantity(+qty).reason(reason)
                .build());
        return inv;
    }

    @Transactional
    public Inventory deallocate(Long inventoryId, long qty, String reason) {
        Inventory inv = get(inventoryId);
        inv.deallocate(qty);
        ledgerRepository.save(InventoryLedger.builder()
                .inventory(inv).eventType(LedgerEventType.DEALLOCATE).quantity(-qty).reason(reason)
                .build());
        return inv;
    }

    @Transactional
    public Inventory ship(Long inventoryId, long qty, String reason) {
        Inventory inv = get(inventoryId);
        inv.deallocate(qty);
        inv.decreaseOnHand(qty);
        ledgerRepository.save(InventoryLedger.builder()
                .inventory(inv).eventType(LedgerEventType.SHIP).quantity(-qty).reason(reason)
                .build());
        return inv;
    }

    private Inventory get(Long id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new ProductException(ProductErrorCode.NOT_FOUND_INVENTORY, id));
    }

    @Transactional(readOnly = true)
    public Inventory getById(Long inventoryId) {
        return get(inventoryId); // 기존 private get 재사용
    }

    @Transactional(readOnly = true)
    public Inventory getByKey(Long productId, String skuCode) {
        if ((productId == null || productId <= 0) && (skuCode == null || skuCode.isBlank())) {
            throw new ProductException(ProductErrorCode.AT_LEAST_ONE_PRODUCTID_OR_SKU_CODE);
        }

        // 우선순위: productId+skuCode → skuCode 단독 → productId 단독(여러개면 1개 강제 X: 필요시 목록 API로 분리)
        if (productId != null && productId > 0 && skuCode != null && !skuCode.isBlank()) {
            return inventoryRepository.findByProductIdAndSkuCode(productId, skuCode)
                    .orElseThrow(() -> new ProductException(ProductErrorCode.NOT_FOUND_INVENTORY,
                            "productId=" + productId + ", sku=" + skuCode));
        }
        if (skuCode != null && !skuCode.isBlank()) {
            return inventoryRepository.findFirstBySkuCodeOrderByIdAsc(skuCode)
                    .orElseThrow(() -> new ProductException(ProductErrorCode.NOT_FOUND_INVENTORY,
                            skuCode));
        }
        return inventoryRepository.findFirstByProductIdOrderByIdAsc(productId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.NOT_FOUND_INVENTORY,
                        productId));
    }
}
