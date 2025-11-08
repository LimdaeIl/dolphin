package com.book.dolphin.common.response;


/**
 * 공통 응답 코드.
 *
 * <p>성공/실패의 <b>논리적 상태</b>를 표현합니다. 실패(예외) 응답은
 * {@link com.book.dolphin.common.response.ErrorResponse} 스키마를 사용하고,
 * 성공 응답은 {@code ApiResponse<T>}를 사용합니다.</p>
 *
 * @apiNote API의 HTTP Status와 1:1 매핑되지 않습니다.
 *  <li>(예: 검증 실패는 HTTP 400 + ErrorResponse, 성공은 HTTP 200 + ApiResponse)</li>
 * @since 1.0
 */
public enum ResultCode {
    SUCCESS, ERROR
}