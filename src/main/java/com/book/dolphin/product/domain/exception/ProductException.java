package com.book.dolphin.product.domain.exception;

import com.book.dolphin.common.exception.AppException;

public class ProductException extends AppException {

    public ProductException(ProductErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public ProductException(ProductErrorCode errorCode) {
        super(errorCode);
    }
}
