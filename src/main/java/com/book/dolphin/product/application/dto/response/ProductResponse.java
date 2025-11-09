package com.book.dolphin.product.application.dto.response;

import java.util.List;

public record ProductResponse(
        Long id,
        String name,
        String content,

        String status,
        Long currentPriceWon,
        List<CategoryBrief> categories,
        List<MediaBrief> representatives,
        List<MediaBrief> contents
) {

    public record CategoryBrief(Long id, String name, boolean primary, int sortKey) {

    }

    public record MediaBrief(Long id, String url, String altText, int sortKey) {

    }
}
