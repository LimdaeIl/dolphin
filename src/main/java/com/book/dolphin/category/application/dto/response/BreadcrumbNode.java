package com.book.dolphin.category.application.dto.response;

public record BreadcrumbNode(
        Long id,
        String name,
        String slug
) {}
