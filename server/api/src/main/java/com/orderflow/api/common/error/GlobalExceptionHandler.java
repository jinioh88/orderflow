package com.orderflow.api.common.error;

import com.orderflow.api.common.response.ErrorResponse;
import com.orderflow.common.error.BusinessException;
import com.orderflow.common.error.CatalogErrorCode;
import com.orderflow.common.error.CommonErrorCode;
import com.orderflow.common.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * 전역 예외 → 에러 응답 변환 — api-spec.md 1.3/1.4의 단일 구현 지점.
 * 여기 없는 매핑을 새로 추가할 때는 스펙 1.4 표부터 갱신한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 예외가 details를 실어 오면(엑셀 행 오류 등) 그대로 직렬화한다 — 기능별 핸들러 분기 없음 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        List<ErrorResponse.FieldError> details = e.details().stream()
                .map(detail -> new ErrorResponse.FieldError(detail.row(), detail.field(), detail.reason()))
                .toList();
        return ResponseEntity.status(errorCode.status())
                .body(new ErrorResponse(new ErrorResponse.ErrorBody(
                        errorCode.code(), e.getMessage(), details.isEmpty() ? null : details)));
    }

    /** 업로드 용량 초과 — 스펙상 상한 초과는 EXCEL_FILE_INVALID (api-spec 3.3.1). 상한 수치는 설정 소관이라 재기술하지 않는다 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(CatalogErrorCode.EXCEL_FILE_INVALID.status())
                .body(ErrorResponse.of(CatalogErrorCode.EXCEL_FILE_INVALID, "업로드 용량 제한을 초과했습니다."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<ErrorResponse.FieldError> details = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> ErrorResponse.FieldError.of(
                        fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(CommonErrorCode.VALIDATION_ERROR.status())
                .body(ErrorResponse.of(CommonErrorCode.VALIDATION_ERROR, details));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidRequest(Exception e) {
        return ResponseEntity.status(CommonErrorCode.INVALID_REQUEST.status())
                .body(ErrorResponse.of(CommonErrorCode.INVALID_REQUEST));
    }

    /** 유니크 제약 등 DB 정합성 위반 — 사전 검사 사이의 레이스 안전망 (예: 이메일 전역 유일) */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            org.springframework.dao.DataIntegrityViolationException e) {
        return ResponseEntity.status(CommonErrorCode.CONFLICT.status())
                .body(ErrorResponse.of(CommonErrorCode.CONFLICT));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(CommonErrorCode.RESOURCE_NOT_FOUND.status())
                .body(ErrorResponse.of(CommonErrorCode.RESOURCE_NOT_FOUND));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(CommonErrorCode.METHOD_NOT_ALLOWED.status())
                .body(ErrorResponse.of(CommonErrorCode.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("처리되지 않은 예외", e);
        return ResponseEntity.status(CommonErrorCode.INTERNAL_ERROR.status())
                .body(ErrorResponse.of(CommonErrorCode.INTERNAL_ERROR));
    }
}
