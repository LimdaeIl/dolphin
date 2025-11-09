package com.book.dolphin.product.application.dto.response;


import com.book.dolphin.product.domain.entity.Inventory;
import java.time.LocalDateTime;

public record InventoryResponse(
        Long id,
        Long productId,
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
        return new InventoryResponse(
                inv.getId(),
                inv.getProduct().getId(),
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