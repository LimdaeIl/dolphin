package com.book.dolphin.category.application.dto.response;

import java.util.List;

public record MegaMenuResponse(
        List<Node> roots,
        SelectedRoot selectedRoot,
        List<Node> childrenOfSelectedRoot
) {

    public record Node(
            Long id,
            String name,
            String slug,
            String imageUrl,
            Integer childCount
    ) {

    }

    public record SelectedRoot(
            Long id,
            String name,
            String slug,
            String imageUrl
    ) {

    }
}