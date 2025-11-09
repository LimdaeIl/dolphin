package com.book.dolphin.product.domain.exception;

import com.book.dolphin.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProductErrorCode implements ErrorCode {

    REPRESENTATIVE_CATEGORY_ONLY_ONE(HttpStatus.BAD_REQUEST, "대표 카테고리는 상품당 1개만 설정할 수 있습니다."),
    REPRESENTATIVE_IMAGE_MAX_OVER(HttpStatus.BAD_REQUEST, "대표 이미지는 최대 8장까지 등록할 수 있습니다. 요청 개수=%s"),
    DUPLICATE_SALE_PERIOD(HttpStatus.CONFLICT, "기존 할인 기간과 겹칩니다."),
    CANNOT_PUBLISH_WITH_INACTIVE_CATEGORY(HttpStatus.BAD_REQUEST,
            "비활성 카테고리가 연결되어 있어 상품을 공개할 수 없습니다."),
    INVALID_SALE_PERIOD(HttpStatus.BAD_REQUEST, "할인 기간이 올바르지 않습니다. (종료가 시작보다 이릅니다)"),
    SALE_NOT_CHEAPER_THAN_LIST(HttpStatus.BAD_REQUEST, "세일가는 정가보다 작아야 합니다."),
    DUPLICATE_CATEGORY(HttpStatus.BAD_REQUEST, "카테고리가 중복되었습니다. categoryId=%s"),
    PRICE_REQUIRED(HttpStatus.BAD_REQUEST, "가격 정보는 필수입니다."),
    INVALID_DATETIME_FORMAT(HttpStatus.BAD_REQUEST, "지원하지 않는 시간 형식입니다. 예: 2025-11-10T00:00:00"),
    NOT_FOUND_INVENTORY(HttpStatus.NOT_FOUND, "재고를 찾을 수 없습니다: %s"),
    NOT_FOUND_PRODUCT(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    ALREADY_EXISTS_INVENTORY(HttpStatus.CONFLICT,
            "이미 존재하는 재고입니다. productId=%s, sku=%s"),
    INVENTORY_OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "재고가 부족하여 할당할 수 없습니다. 요청 수량: %s"),
    INCORRECT_DEALLOCATION_QUANTITY(HttpStatus.BAD_REQUEST, "잘못된 할당 해제 수량입니다. 할당 해제 수량: %s"),
    QUANTITY_IN_STOCK_MINIMUM_ONE(HttpStatus.BAD_REQUEST, "입고 수량은 최소 1이상이어야만 합니다: %s"),
    DEDUCT_MORE_THEN_AMOUNT(HttpStatus.BAD_REQUEST, "보유수량보다 많이 차감할 수 없습니다: %s"), 
    AT_LEAST_ONE_PRODUCTID_OR_SKU_CODE(HttpStatus.BAD_REQUEST, "productId 또는 skuCode는 최소 하나가 필요합니다.");


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
