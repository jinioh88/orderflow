package com.orderflow.common.error;

/**
 * AUTH 에픽 에러 코드 — api-spec.md 2.5와 1:1로 대응한다.
 */
public enum AuthErrorCode implements ErrorCode {

    INVALID_CREDENTIALS(401, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(401, "유효하지 않은 토큰입니다. 다시 로그인해 주세요."),
    TOKEN_EXPIRED(401, "인증이 만료되었습니다."),
    ACCOUNT_INACTIVE(403, "이용할 수 없는 계정입니다."),
    PASSWORD_SETUP_REQUIRED(403, "비밀번호 설정이 필요합니다."),
    EMAIL_DUPLICATED(409, "이미 사용 중인 이메일입니다."),
    STORE_INACTIVE(409, "비활성화된 가맹점입니다.");

    private final int status;
    private final String message;

    AuthErrorCode(int status, String message) {
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
