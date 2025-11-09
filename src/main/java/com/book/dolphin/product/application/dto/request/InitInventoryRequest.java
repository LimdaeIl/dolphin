package com.book.dolphin.product.application.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InitInventoryRequest(
        @NotNull Long variantId,
        @Min(0) long onHand,
        @Min(0) long safetyStock,
        @NotNull boolean backorderable
) {

}
