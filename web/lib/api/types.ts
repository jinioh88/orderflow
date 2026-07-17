// api-spec.md 1.3~1.5 공통 규약 타입.
// 스펙이 '초안' 상태이므로 규약 변경 시 이 파일만 고치면 되도록 여기에만 정의한다.

/** 성공 응답 래퍼 — 최상위 키는 `data` 하나 (1.3) */
export interface ApiSuccessBody<T> {
  data: T;
}

/** 실패 응답의 필드 단위 오류 (1.3 `details`) */
export interface FieldErrorDetail {
  field: string;
  reason: string;
}

/** 실패 응답 래퍼 — 최상위 키는 `error` 하나 (1.3) */
export interface ApiErrorBody {
  error: {
    code: string;
    message: string;
    details?: FieldErrorDetail[];
  };
}

/**
 * 서버가 정의한 공통 에러 코드 (1.4). 도메인 코드는 각 에픽 스펙에서 추가되므로
 * 이 목록은 닫혀 있지 않다 — 분기는 항상 `HTTP 상태 + code` 문자열로 한다.
 */
export const API_ERROR_CODES = {
  INVALID_REQUEST: "INVALID_REQUEST",
  VALIDATION_ERROR: "VALIDATION_ERROR",
  UNAUTHORIZED: "UNAUTHORIZED",
  TOKEN_EXPIRED: "TOKEN_EXPIRED",
  FORBIDDEN: "FORBIDDEN",
  RESOURCE_NOT_FOUND: "RESOURCE_NOT_FOUND",
  METHOD_NOT_ALLOWED: "METHOD_NOT_ALLOWED",
  CONFLICT: "CONFLICT",
  INTERNAL_ERROR: "INTERNAL_ERROR",
} as const;

/** 서버 응답 없이 클라이언트에서 만들어지는 합성 코드 — status는 0이다 */
export const CLIENT_ERROR_CODES = {
  /** fetch 자체가 실패 (네트워크 단절, CORS 등) */
  NETWORK_ERROR: "NETWORK_ERROR",
  /** 응답 본문이 공통 래퍼 형태가 아님 */
  UNEXPECTED_RESPONSE: "UNEXPECTED_RESPONSE",
} as const;

/**
 * API 호출 실패를 나타내는 유일한 예외 타입.
 * 훅/컴포넌트 레이어는 `instanceof ApiError` 후 `status`와 `code`로만 분기한다.
 * `message`는 표시용일 뿐 계약이 아니다 (1.3).
 */
export class ApiError extends Error {
  /** HTTP 상태. 서버 응답이 없으면(네트워크 오류 등) 0 */
  readonly status: number;
  readonly code: string;
  readonly details?: FieldErrorDetail[];

  constructor(
    status: number,
    code: string,
    message: string,
    details?: FieldErrorDetail[],
  ) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.details = details;
  }

  /** `VALIDATION_ERROR` 등의 details를 `{ field: reason }` 맵으로 변환 — 폼 필드 에러 매핑용 */
  fieldErrors(): Record<string, string> {
    return Object.fromEntries(
      (this.details ?? []).map((d) => [d.field, d.reason]),
    );
  }
}

/** 페이징 요청 쿼리 (1.5) */
export interface PageQuery {
  /** 0부터 시작, 기본 0 */
  page?: number;
  /** 기본 20, 최대 100 */
  size?: number;
  /** `<필드>,asc|desc` — 정렬 허용 필드는 각 API 스펙에 명시 */
  sort?: string;
}

/** 페이징 응답의 `data` 형태 (1.5) */
export interface PageResponse<T> {
  items: T[];
  page: {
    number: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}
