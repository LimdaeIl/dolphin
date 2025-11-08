package com.book.dolphin.common.exception;

import com.book.dolphin.common.response.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


/**
 * 애플리케이션 전역 예외를 표준 에러 응답으로 매핑한다.
 *
 * <p>매핑 규칙:
 * <ul>
 *   <li>{@link MethodArgumentNotValidException} → 400 + VALIDATION_FAILED + 필드별 오류</li>
 *   <li>{@link EntityNotFoundException} → 404 + ENTITY_NOT_FOUND</li>
 *   <li>그 외 처리되지 않은 예외 → 500 + INTERNAL_SERVER_ERROR</li>
 * </ul>
 *
 * @apiNote 응답 본문은 {@link com.book.dolphin.common.response.ErrorResponse} 형식으로 고정됩니다.
 * @implNote PII/민감 정보를 message에 노출하지 않도록 주의하세요. rootCause는 로그에만 남깁니다.
 * @since 1.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {


    /**
     * 도메인 전용 예외를 매핑한다.
     *
     * <p>예외가 보유한 {@link ErrorCode}의 HTTP 상태와 포맷된 메시지를 그대로 응답한다.</p>
     *
     * @param ex 도메인 예외
     * @return 상태코드 = {@link ErrorCode#getHttpStatus()}, 메시지 = {@link Exception#getMessage()}
     * @apiNote 클라이언트에 노출 가능한 사용자 메시지만 포맷 인자로 넣어야 한다.
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleApp(AppException ex) {
        ErrorCode code = ex.getErrorCode();
        return ResponseEntity.status(code.getHttpStatus())
                .body(ErrorResponse.of(code.getHttpStatus(), ex.getMessage())); // 포맷된 메시지
    }

    /**
     * {@code @Valid} 바인딩 실패를 400과 필드 오류 목록으로 매핑한다.
     *
     * @param ex 스프링 바인딩 검증 예외
     * @return 400 + {@code data=[{field, reason}...]}
     * @apiNote 필드 경로는 스프링 바인딩 규칙(field) 그대로 제공한다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalid(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> ErrorResponse.FieldError.of(err.getField(), err.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST,
                        AppErrorCode.INVALID_INPUT_VALUE.getMessageTemplate(),
                        errors));
    }

    /**
     * Bean Validation 직접 호출 시의 제약 위반을 400으로 매핑한다.
     *
     * @param ex 제약 위반 예외
     * @return 400 + {@code data=[{fieldPath, message}...]}
     * @apiNote propertyPath는 구현체에 따라 경로 표기법이 다를 수 있다.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex) {
        List<ErrorResponse.FieldError> errors = ex.getConstraintViolations().stream()
                .map(v -> ErrorResponse.FieldError.of(
                        v.getPropertyPath().toString(),
                        v.getMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST,
                        AppErrorCode.INVALID_INPUT_VALUE.getMessageTemplate(),
                        errors));
    }


    /**
     * 잘못된 JSON 본문(파싱 실패/형식 오류)을 400으로 매핑한다.
     *
     * @param ex 메시지 읽기 실패 예외
     * @return 400 + 일반화된 메시지
     * @implNote 구체 원인은 로그로만 남기고 응답에는 노출하지 않는다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST,
                        AppErrorCode.INVALID_INPUT_VALUE.getMessageTemplate()));
    }

    /**
     * 지원하지 않는 HTTP 메서드를 405로 매핑한다.
     *
     * @param ex 메서드 미지원 예외
     * @return 405 + 일반화된 메시지
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.of(
                        HttpStatus.METHOD_NOT_ALLOWED,
                        AppErrorCode.METHOD_NOT_ALLOWED.getMessageTemplate()));
    }


    /**
     * JPA 엔티티 미발견을 404로 매핑한다.
     *
     * @param ex 엔티티 미발견 예외
     * @return 404 + 일반화된 메시지
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(
                        HttpStatus.NOT_FOUND,
                        AppErrorCode.ENTITY_NOT_FOUND.getMessageTemplate()));
    }

    /**
     * 응답 직렬화 실패를 500으로 매핑한다.
     *
     * @param ex 본문 쓰기 실패 예외
     * @return 500 + 일반화된 메시지
     * @implNote 원인 메시지는 서버 로그에만 기록한다.
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotWritableException.class)
    public ResponseEntity<ErrorResponse> handleNotWritable(org.springframework.http.converter.HttpMessageNotWritableException ex) {
        log.error("응답 직렬화 실패: {}", rootCauseMessage(ex), ex);
        return ResponseEntity.status(AppErrorCode.RESP_BODY_WRITE_ERROR.getHttpStatus())
                .body(ErrorResponse.of(
                        AppErrorCode.RESP_BODY_WRITE_ERROR.getHttpStatus(),
                        AppErrorCode.RESP_BODY_WRITE_ERROR.getMessageTemplate()));
    }

    /**
     * 콘텐츠 협상 실패를 406으로 매핑한다.
     *
     * @param ex 수용 불가 예외
     * @return 406 + 일반화된 메시지
     */
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponse> handleNotAcceptable(org.springframework.web.HttpMediaTypeNotAcceptableException ex) {
        log.warn("콘텐츠 협상 실패(406): {}", rootCauseMessage(ex));
        return ResponseEntity.status(AppErrorCode.MEDIA_TYPE_NOT_ACCEPTABLE.getHttpStatus())
                .body(ErrorResponse.of(
                        AppErrorCode.MEDIA_TYPE_NOT_ACCEPTABLE.getHttpStatus(),
                        AppErrorCode.MEDIA_TYPE_NOT_ACCEPTABLE.getMessageTemplate()));
    }

    /**
     * 미지원 Content-Type을 415로 매핑한다.
     *
     * @param ex 미지원 미디어 타입 예외
     * @return 415 + 일반화된 메시지
     */
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupported(org.springframework.web.HttpMediaTypeNotSupportedException ex) {
        log.warn("미지원 콘텐츠 타입(415): {}", rootCauseMessage(ex));
        return ResponseEntity.status(AppErrorCode.UNSUPPORTED_MEDIA_TYPE.getHttpStatus())
                .body(ErrorResponse.of(
                        AppErrorCode.UNSUPPORTED_MEDIA_TYPE.getHttpStatus(),
                        AppErrorCode.UNSUPPORTED_MEDIA_TYPE.getMessageTemplate()));
    }


    /**
     * 처리되지 않은 모든 예외를 500으로 매핑하는 안전망 핸들러.
     *
     * @param ex 포착되지 않은 예외
     * @return 500 + 일반화된 메시지
     * @implNote 내부 원인은 로그에만 남기며, 응답에는 일반화된 메시지를 사용한다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(Exception ex) {
        log.error("처리되지 않은 예외: {}", rootCauseMessage(ex), ex);
        return ResponseEntity.status(AppErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ErrorResponse.of(
                        AppErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus(),
                        AppErrorCode.INTERNAL_SERVER_ERROR.getMessageTemplate()));
    }

    /**
     * 예외 체인의 루트 원인 메시지를 추출한다.
     *
     * @param t 예외
     * @return 루트 cause의 메시지(없으면 {@code null})
     * @implNote 응답 본문에 사용하지 말고, 로깅 등 내부 용도로만 사용한다.
     */
    private static String rootCauseMessage(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null) c = c.getCause();
        return c.getMessage();
    }
}
