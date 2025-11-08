package com.book.dolphin.common.exception;

import lombok.Getter;

/**
 * 애플리케이션 공통 런타임 예외.
 *
 * <p>{@link ErrorCode}와 선택적 포맷 인자를 받아, 메시지를 즉시 포맷해
 * 상위 핸들러에서 {@link com.book.dolphin.common.response.ErrorResponse}로 변환됩니다.</p>
 *
 * @implNote 생성 시 {@code getMessage()}는 이미 포맷된 문자열입니다. 민감정보를 포맷 인자로 전달하지 마세요.
 * @since 1.0
 */
@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * 메시지 포맷 인자(직렬화 제외).
     */
    private final transient Object[] args;


    /**
     * 템플릿 + 값 바인딩 생성자.
     *
     * @param errorCode 에러 코드
     * @param args      템플릿 포맷 인자
     */
    public AppException(ErrorCode errorCode, Object... args) {
        super(errorCode.format(args));
        this.errorCode = errorCode;
        this.args = args;
    }

    /**
     * 값 바인딩 없이 고정 메시지 사용.
     *
     * @param errorCode 에러 코드
     */
    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessageTemplate());
        this.errorCode = errorCode;
        this.args = null;
    }
}
