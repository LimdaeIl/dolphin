package com.book.dolphin.category.domain.exception;

import com.book.dolphin.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CategoryErrorCode implements ErrorCode {
    PARENT_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리: 부모 카테고리를 찾을 수 없습니다: %s"),
    DUPLICATE_SLUG_BY_ROOT(HttpStatus.CONFLICT, "카테고리: 중복된 슬러그 입니다: %s"),
    DUPLICATE_SLUG_BY_PARENT(HttpStatus.CONFLICT, "카테고리: 해당 부모 아래 동일한 슬러그가 이미 존재합니다. parentId: %d slug: %s"),
    EMPTY_SLUG(HttpStatus.BAD_REQUEST, "카테고리: 빈 슬러그 입니다."),

    ALREADY_PATH(HttpStatus.CONFLICT, "카테고리: 이미 존재하는 경로 입니다: %s");


    private final HttpStatus httpStatus;
    private final String messageTemplate;

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getMessageTemplate() {
        return messageTemplate;
    }
}