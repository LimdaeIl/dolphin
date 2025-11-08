package com.book.dolphin.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 표준 에러 코드 계약.
 *
 * <p>각 도메인/시스템 오류를 HTTP 상태와 메시지 템플릿으로 정의합니다.</p>
 *
 * @apiNote {@link #getMessageTemplate()}는 {@link String#format(String, Object...)} 호환 템플릿입니다. 값
 * 바인딩이 필요 없으면 고정 메시지로만 사용하세요.
 * @since 1.0
 */
public interface ErrorCode {

    /**
     * 매핑될 HTTP 상태.
     */
    HttpStatus getHttpStatus();

    /**
     * 사용자 메시지 템플릿(예: {@code "카테고리(%s)를 찾을 수 없습니다."}).
     */
    String getMessageTemplate();

    /**
     * 템플릿에 값을 바인딩합니다.
     *
     * @param args 템플릿 포맷 인자
     * @return 포맷된 메시지
     */
    default String format(Object... args) {
        return String.format(getMessageTemplate(), args);
    }
}
