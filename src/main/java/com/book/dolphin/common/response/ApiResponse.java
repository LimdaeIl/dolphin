package com.book.dolphin.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 공통 성공/실패 응답 래퍼.
 *
 * <p>성공: {@code { "code": "SUCCESS", "message": "OK", "data": ... }}
 * <br/>실패는 {@link ErrorResponse}를 사용합니다(응답 스키마 분리).
 *
 * @param <T> payload 타입
 * @apiNote 성공 응답만 이 클래스를 통해 반환합니다. 오류는 ErrorResponse 사용.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        ResultCode code,
        String message,
        T data
) {
    /* ---- SUCCESS ---- */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResultCode.SUCCESS, "OK", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(ResultCode.SUCCESS, message, data);
    }

    /* ---- ERROR ---- */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(ResultCode.ERROR, message, null);
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(ResultCode.ERROR, message, data);
    }
}