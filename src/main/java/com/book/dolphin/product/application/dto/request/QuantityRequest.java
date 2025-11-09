package com.book.dolphin.product.application.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record QuantityRequest(
        @Positive long quantity,
        @Size(max = 255) String reason
) {}
