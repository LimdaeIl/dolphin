package com.book.dolphin.product.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProductCreateRequest(
        @NotBlank @Size(max = 100)
        String name,

        @NotBlank // 본문(마크다운/HTML 허용)
        String content,

        @NotBlank @Size(max = 64)
        String skuCode,

        @Valid @NotNull
        PricePayload price,                // 상시가 + (선택)할인가

        @Valid
        List<CategoryAssign> categories,   // 선택: 연결할 카테고리들

        @Valid
        List<MediaPayload> representatives,// 선택: 대표 이미지(최소 5, 최대 8장 정책 적용)

        @Valid
        List<MediaPayload> contents        // 선택: 본문 삽입 이미지(개수 제한 X)
) {

    public record PricePayload(
            @NotNull
            @Positive
            Long listPriceWon,                 // KRW 정가 (원 단위 정수)

            @Positive
            Long salePriceWon,                 // 선택: 할인가(원)

            String saleFrom,                   // ISO-8601("2025-11-09T00:00:00+09:00") or null
            String saleUntil                   // ISO-8601 or null
    ) {

    }

    public record CategoryAssign(
            @NotNull Long categoryId,
            boolean primary,
            @Min(0) int sortKey
    ) {

    }

    public record MediaPayload(
            @NotBlank
            @Size(max = 1024) String url,
            @Size(max = 255) String altText,
            @Min(0) int sortKey
    ) {

    }
}