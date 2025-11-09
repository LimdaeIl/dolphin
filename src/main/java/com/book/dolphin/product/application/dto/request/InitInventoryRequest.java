package com.book.dolphin.product.application.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InitInventoryRequest(
        @NotNull Long productId,
        @NotBlank @Size(max = 64) String skuCode,
        @Min(0) long onHand,
        @Min(0) long safetyStock,
        boolean backorderable
) {

}
