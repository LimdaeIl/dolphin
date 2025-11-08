package com.book.dolphin.category.domain.exception;

import com.book.dolphin.common.exception.AppException;

public class CategoryException extends AppException {

    public CategoryException(CategoryErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public CategoryException(CategoryErrorCode errorCode) {
        super(errorCode);
    }
}
