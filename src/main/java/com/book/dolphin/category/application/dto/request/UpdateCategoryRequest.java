package com.book.dolphin.category.application.dto.request;

import com.book.dolphin.category.domain.entity.CategoryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(

        @Size(max = 50, message = "카테고리: 카테고리 이름 길이는 최대 50글자 입니다.")
        String name,

        @Size(max = 1024, message = "카테고리: 이미지 경로 길이는 최대 1024글자 입니다.")
        String imageUrl,

        @PositiveOrZero(message = "카테고리: 정렬 순서는 숫자만 입력할 수 있습니다.")
        Integer sortOrder,

        CategoryStatus status
) {

}
