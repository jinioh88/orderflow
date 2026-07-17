package com.orderflow.common.error;

/**
 * 에러 코드 계약 — api-spec.md 1.4의 code/HTTP 상태 쌍.
 * 공통 코드는 {@link CommonErrorCode}, 도메인별 코드는 각 컨텍스트가 이 인터페이스를 구현해 추가한다.
 */
public interface ErrorCode {

    /** 기계 판별용 코드 (SCREAMING_SNAKE) — 클라이언트 분기 기준이므로 한 번 공개되면 바꾸지 않는다. */
    String code();

    /** 표시용 기본 메시지 — 문구는 계약이 아니다. */
    String message();

    /** HTTP 상태 코드. common 모듈은 프레임워크 비의존이므로 int로 둔다. */
    int status();
}
