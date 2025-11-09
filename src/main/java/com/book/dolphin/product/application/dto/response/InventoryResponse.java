package com.book.dolphin.product.application.dto.response;


import com.book.dolphin.product.domain.entity.Inventory;
import com.book.dolphin.product.domain.entity.ProductVariant;
import java.time.LocalDateTime;


public record InventoryResponse(
        Long id,
        Long productId,
        Long variantId,
        String skuCode,
        long onHand,
        long allocated,
        long safetyStock,
        boolean backorderable,
        long available,
        long version,
        LocalDateTime updatedAt
) {
    public static InventoryResponse of(Inventory inv) {
        ProductVariant v = inv.getVariant();
        return new InventoryResponse(
                inv.getId(),
                v.getProduct().getId(),
                v.getId(),
                inv.getSkuCode(),
                inv.getOnHand(),
                inv.getAllocated(),
                inv.getSafetyStock(),
                inv.isBackorderable(),
                inv.available(),
                inv.getVersion(),
                inv.getUpdatedAt()
        );
    }
}
