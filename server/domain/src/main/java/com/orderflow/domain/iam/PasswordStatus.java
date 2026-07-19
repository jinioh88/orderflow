package com.orderflow.domain.iam;

/** 임시 비밀번호 상태 (US-AUTH-02·03, PM 2026-07-19). TEMPORARY면 비밀번호 설정 외 API 접근 차단. */
public enum PasswordStatus {
    TEMPORARY, CONFIRMED
}
