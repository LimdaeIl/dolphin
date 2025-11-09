package com.book.dolphin.category.domain.exception;

import com.book.dolphin.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CategoryErrorCode implements ErrorCode {
    PARENT_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리: 부모 카테고리를 찾을 수 없습니다: %s"),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리: 카테고리를 찾을 수 없습니다: %s"),
    DUPLICATE_SLUG_BY_ROOT(HttpStatus.CONFLICT, "카테고리: 중복된 슬러그 입니다: %s"),
    DUPLICATE_SLUG_BY_PARENT(HttpStatus.CONFLICT,
            "카테고리: 해당 부모 아래 동일한 슬러그가 이미 존재합니다. parentId: %d slug: %s"),
    EMPTY_SLUG(HttpStatus.BAD_REQUEST, "카테고리: 빈 슬러그 입니다."),
    MAX_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "카테고리: 최대 깊이를 초과했습니다. 최대 깊이: %s"),

    ALREADY_PATH(HttpStatus.CONFLICT, "카테고리: 이미 존재하는 경로입니다: %s"),
    NAME_NOT_NULL(HttpStatus.BAD_REQUEST, "카테고리: 카테고리 이름은 비어 있을 수 없습니다."),
    SORT_ORDER_GREATER_OR_EQUAL_ZERO(HttpStatus.BAD_REQUEST,
            "카테고리: 정렬 순서는 0 이상이어야 합니다."),
    INVALID_REPARENT_TARGET(HttpStatus.BAD_REQUEST,
            "카테고리: 자신의 하위로 이동할 수 없습니다: id=%s, newParentId=%s"),
    INVALID_REPARENT_SELF(HttpStatus.BAD_REQUEST, "카테고리: 부모를 자기 자신으로 설정할 수 없습니다."),
    DEPTH_GREATER_OR_EQUAL_ZERO(HttpStatus.BAD_REQUEST, "카테고리: depth는 0 이상이어야 합니다."),
    PATH_NOT_NULL(HttpStatus.BAD_REQUEST, "카테고리: 경로는 비어 있을 수 없습니다."),
    PATH_LENGTH_MAX_OVER(HttpStatus.BAD_REQUEST, "카테고리: 경로 최대 길이를 초과했습니다."), SUB_TREE_INCONSISTENCY(
            HttpStatus.BAD_REQUEST, "카테고리: 서브 트리가 일치하지 않습니다: %s");

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