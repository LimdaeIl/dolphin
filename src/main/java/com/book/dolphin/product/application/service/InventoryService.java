package com.book.dolphin.product.application.service;

import com.book.dolphin.product.application.dto.response.InventoryResponse;
import com.book.dolphin.product.domain.entity.Inventory;
import com.book.dolphin.product.domain.entity.InventoryLedger;
import com.book.dolphin.product.domain.entity.InventoryLedger.LedgerEventType;
import com.book.dolphin.product.domain.entity.ProductVariant;
import com.book.dolphin.product.domain.exception.ProductErrorCode;
import com.book.dolphin.product.domain.exception.ProductException;
import com.book.dolphin.product.domain.repository.InventoryLedgerRepository;
import com.book.dolphin.product.domain.repository.InventoryRepository;
import com.book.dolphin.product.domain.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryLedgerRepository ledgerRepository;
    private final ProductVariantRepository variantRepository;

    // 초기화: variantId만 받는다. skuCode는 variant에서 가져와 캐싱한다.
    @Transactional
    public InventoryResponse init(Long variantId, long onHand, long safetyStock,
            boolean backorderable) {
        if (variantId == null || variantId <= 0) {
            throw new ProductException(ProductErrorCode.INVALID_VARIANT_ID, variantId);
        }
        if (inventoryRepository.existsByVariantId(variantId)) {
            throw new ProductException(ProductErrorCode.ALREADY_EXISTS_INVENTORY, variantId);
        }

        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(
                        () -> new ProductException(ProductErrorCode.NOT_FOUND_VARIANT, variantId));

        Inventory inv = Inventory.of(variant, onHand, safetyStock, backorderable);
        inventoryRepository.save(inv);
        return InventoryResponse.of(inv);
    }

    @Transactional
    public InventoryResponse inbound(Long inventoryId, long qty, String reason) {
        if (qty <= 0) {
            throw new ProductException(ProductErrorCode.INVALID_QUANTITY_ONLY_POSITIVE, qty);
        }
        Inventory inv = get(inventoryId);
        inv.increaseOnHand(qty);
        ledgerRepository.save(InventoryLedger.builder()
                .inventory(inv).eventType(LedgerEventType.INBOUND).quantity(+qty).reason(reason)
                .build());
        return InventoryResponse.of(inv);
    }

    @Transactional
    public InventoryResponse allocate(Long inventoryId, long qty, String reason) {
        if (qty <= 0) {
            throw new ProductException(ProductErrorCode.INVALID_QUANTITY_ONLY_POSITIVE, qty);
        }
        Inventory inv = get(inventoryId);
        inv.allocate(qty);
        ledgerRepository.save(InventoryLedger.builder()
                .inventory(inv).eventType(LedgerEventType.ALLOCATE).quantity(+qty).reason(reason)
                .build());
        return InventoryResponse.of(inv);
    }

    @Transactional
    public InventoryResponse deallocate(Long inventoryId, long qty, String reason) {
        if (qty <= 0) {
            throw new ProductException(ProductErrorCode.INVALID_QUANTITY_ONLY_POSITIVE, qty);
        }
        Inventory inv = get(inventoryId);
        inv.deallocate(qty);
        ledgerRepository.save(InventoryLedger.builder()
                .inventory(inv).eventType(LedgerEventType.DEALLOCATE).quantity(-qty).reason(reason)
                .build());
        return InventoryResponse.of(inv);
    }

    @Transactional
    public InventoryResponse ship(Long inventoryId, long qty, String reason) {
        if (qty <= 0) {
            throw new ProductException(ProductErrorCode.INVALID_QUANTITY_ONLY_POSITIVE, qty);
        }
        Inventory inv = get(inventoryId);
        // 할당 → 출고 순서
        inv.deallocate(qty);
        inv.decreaseOnHand(qty);
        ledgerRepository.save(InventoryLedger.builder()
                .inventory(inv).eventType(LedgerEventType.SHIP).quantity(-qty).reason(reason)
                .build());
        return InventoryResponse.of(inv);
    }

    private Inventory get(Long id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new ProductException(ProductErrorCode.NOT_FOUND_INVENTORY, id));
    }

    @Transactional(readOnly = true)
    public InventoryResponse getById(Long inventoryId) {
        return InventoryResponse.of(get(inventoryId));
    }

    // 조회 키: variantId 우선, 없으면 skuCode(전역 캐시값)로 첫 건
    @Transactional(readOnly = true)
    public InventoryResponse getByKey(Long variantId, String skuCode) {
        if ((variantId == null || variantId <= 0) && (skuCode == null || skuCode.isBlank())) {
            throw new ProductException(ProductErrorCode.AT_LEAST_ONE_VARIANTID_OR_SKU_CODE);
        }
        if (variantId != null && variantId > 0) {
            Inventory inv = inventoryRepository.findByVariantId(variantId)
                    .orElseThrow(() -> new ProductException(ProductErrorCode.NOT_FOUND_INVENTORY,
                            variantId));
            return InventoryResponse.of(inv);
        }
        Inventory inv = inventoryRepository.findFirstBySkuCodeOrderByIdAsc(skuCode)
                .orElseThrow(
                        () -> new ProductException(ProductErrorCode.NOT_FOUND_INVENTORY, skuCode));
        return InventoryResponse.of(inv);
    }
}
