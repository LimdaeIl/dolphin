package com.book.dolphin.category.application.dto.request;

import com.book.dolphin.category.domain.entity.CategoryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;

@Builder(access = AccessLevel.PRIVATE)
public record CreateCategoryRequest(

        Long parentId,

        @NotBlank(message = "카테고리: 카테고리 이름은 필수 입니다.")
        @Size(max = 50, message = "카테고리: 카테고리 이름 길이는 최대 50글자 입니다.")
        String name,

        @NotBlank(message = "카테고리: 슬러그는 필수 입니다.")
        @Size(max = 50, message = "카테고리: 카테고리 슬러그는 최대 50글자 입니다.")
        String slug,

        Integer sortOrder,

        CategoryStatus status,

        String imageUrl
) {}
