package com.book.dolphin.category.application.dto.response;

import com.book.dolphin.category.domain.entity.CategoryStatus;

public record CreateCategoryResponse(
        Long id,
        Long parentId,
        String name,
        String slug,
        String path,
        int depth,
        int sortOrder,
        CategoryStatus status
) {

    public static CreateCategoryResponse of(
            Long id, Long parentId, String name, String slug,
            String path, int depth, int sortOrder, CategoryStatus status) {
        return new CreateCategoryResponse(id, parentId, name, slug, path, depth, sortOrder, status);
    }
}