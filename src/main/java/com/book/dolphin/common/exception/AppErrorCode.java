package com.book.dolphin.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;


/**
 * 공통/시스템 영역 에러 코드 모음.
 *
 * <p>각 상수는 <b>HTTP 상태</b>와 <b>메시지 템플릿</b>을 가집니다.
 * 메시지 템플릿은 {@link String#format(String, Object...)} 규칙을 따릅니다.</p>
 *
 * <p>예)
 * <pre>{@code
 * throw new AppException(AppErrorCode.ENTITY_NOT_FOUND);
 * throw new AppException(AppErrorCode.INVALID_INPUT_VALUE, "name");
 * }</pre>
 * </p>
 *
 * @since 1.0
 */
@RequiredArgsConstructor
@Getter
public enum AppErrorCode implements ErrorCode {
    // 시스템/인프라
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "공통: 서버 내부 오류가 발생했습니다."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "공통: 서비스가 일시적으로 불가능합니다."),
    GATEWAY_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "공통: 게이트웨이 응답 시간이 초과되었습니다."),
    DEPENDENCY_FAILURE(HttpStatus.BAD_GATEWAY, "공통: 외부/하위 시스템 연동에 실패했습니다."),
    IO_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "공통: 입출력 처리 중 오류가 발생했습니다."),
    REQUEST_CONTEXT_NOT_FOUND(HttpStatus.NOT_FOUND, "공통: 요청 컨텍스트를 찾을 수 없습니다."),

    // 요청/형식/프로토콜
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "공통: 잘못된 입력입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "공통: 허용되지 않은 HTTP 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "공통: 지원하지 않는 콘텐츠 타입입니다."),
    // 리소스/상태
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "공통: 요청한 리소스를 찾을 수 없습니다."),
    RESP_BODY_WRITE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "공통: 응답 본문을 생성/쓰기 중 오류가 발생했습니다."),
    MEDIA_TYPE_NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "공통: 요청한 응답 형식을 제공할 수 없습니다.");

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
