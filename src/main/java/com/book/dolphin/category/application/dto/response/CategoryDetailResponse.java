package com.book.dolphin.category.application.dto.response;

import java.util.List;

public record CategoryDetailResponse(
        Node category,                 // 현재 카테고리(코어)
        List<BreadcrumbNode> breadcrumb,
        List<Node> children,           // include에 따라 선택
        List<Node> siblings,           // include에 따라 선택
        List<Node> roots               // include에 따라 선택 (헤더 탭/메가메뉴 연동용)
) {
    public record Node(
            Long id,
            String name,
            String slug,
            String path,
            Integer depth,
            String imageUrl
    ) {}

    public record BreadcrumbNode(
            Long id,
            String name,
            String slug
    ) {}
}
