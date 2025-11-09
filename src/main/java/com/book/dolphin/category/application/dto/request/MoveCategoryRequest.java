package com.book.dolphin.category.application.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record MoveCategoryRequest(

        @Positive(message = "카테고리: 이동할 카테고리 부모 ID는 숫자만 입력 가능할 수 있습니다.")
        Long newParentId
) {}
