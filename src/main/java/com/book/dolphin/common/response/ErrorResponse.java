package com.book.dolphin.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * 표준 에러 응답 스키마.
 *
 * <p>모든 예외 상황을 <b>일관된 포맷</b>으로 직렬화해 클라이언트에 제공합니다.</p>
 *
 * <ul>
 *   <li><b>status</b>: HTTP 상태 코드(정수)</li>
 *   <li><b>message</b>: 사람에게 전달할 메시지(민감 정보 포함 금지)</li>
 *   <li><b>error</b>: 도메인 에러 코드(선택) 예: {@code VALIDATION_FAILED}</li>
 *   <li><b>data</b>: 필드 단위 오류 목록(선택)</li>
 * </ul>
 *
 * @apiNote 성공 응답은 {@code ApiResponse<T>}를 사용하세요.
 * @implNote {@code data}는 비어있으면 {@code null}로 직렬화되어 페이로드를 줄입니다.
 *
 * @since 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,              // 기존 code → status (HTTP status)
        String message,          // 사람 읽는 메시지
        String error,            // (선택) 도메인 에러 코드, e.g. CATEGORY_NOT_FOUND
        List<FieldError> data    // 필드 단위 검증 에러 목록
) {

    /**
     * 상태/메시지/필드오류 목록으로 에러 응답을 생성합니다.
     *
     * @param status  HTTP 상태
     * @param message 사용자 메시지(민감정보 금지)
     * @param data    필드 오류 목록(비어있으면 null 처리)
     * @return 표준 에러 응답
     */
    public static ErrorResponse of(HttpStatus status, String message, List<FieldError> data) {
        return new ErrorResponse(status.value(), message, null,
                (data == null || data.isEmpty()) ? null : data);
    }

    /**
     * 상태/메시지로 에러 응답을 생성합니다.
     */
    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(status.value(), message, null, null);
    }

    /**
     * 상태/메시지/도메인 에러코드로 에러 응답을 생성합니다.
     */
    public static ErrorResponse of(HttpStatus status, String message, String errorCode) {
        return new ErrorResponse(status.value(), message, errorCode, null);
    }

    /**
     * 상태/메시지/도메인 에러코드/필드오류 목록으로 에러 응답을 생성합니다.
     */
    public static ErrorResponse of(HttpStatus status, String message, String errorCode,
            List<FieldError> data) {
        return new ErrorResponse(status.value(), message, errorCode,
                (data == null || data.isEmpty()) ? null : data);
    }

    /**
     * 단일 필드 오류.
     *
     * @param field  유효성 검증 대상 필드명(경로 포함 가능)
     * @param reason 실패 사유(로컬라이즈 가능)
     */
    public record FieldError(String field, String reason) {

        public static FieldError of(String field, String reason) {
            return new FieldError(field, reason);
        }
    }
}
