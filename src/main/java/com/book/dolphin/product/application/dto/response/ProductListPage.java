package com.book.dolphin.product.application.dto.response;

import java.util.List;

public record ProductListPage(
        List<ProductListItem> items,
        int page,
        int size,
        long total
) {

}