package com.book.dolphin.product.application.dto.response;

public record ProductListItem(
        Long id,
        String name,
        String status,
        Long currentPrice,
        String representativeImageUrl
) {

}