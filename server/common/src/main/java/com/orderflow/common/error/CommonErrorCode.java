package com.orderflow.common.error;

/**
 * 공통 에러 코드 — api-spec.md 1.4와 1:1로 대응한다.
 * AUTH 도메인 코드(TOKEN_EXPIRED 등)는 {@link AuthErrorCode}에 있다.
 */
public enum CommonErrorCode implements ErrorCode {

    INVALID_REQUEST(400, "요청이 올바르지 않습니다."),
    VALIDATION_ERROR(400, "입력값 검증에 실패했습니다."),
    UNAUTHORIZED(401, "인증에 실패했습니다."),
    FORBIDDEN(403, "접근 권한이 없습니다."),
    RESOURCE_NOT_FOUND(404, "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(405, "허용되지 않은 메서드입니다."),
    CONFLICT(409, "요청이 현재 상태와 충돌합니다."),
    INTERNAL_ERROR(500, "일시적인 오류가 발생했습니다.");

    private final int status;
    private final String message;

    CommonErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public int status() {
        return status;
    }
}
