package com.book.dolphin.category.application.dto.response;

import java.util.List;

public record MoveCategoryResponse(
        Long id,
        Long newParentId,
        String path,
        int depth,
        List<BreadcrumbNode> breadcrumb
) {

}
