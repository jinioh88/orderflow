import {
  API_ERROR_CODES,
  ApiError,
  ApiErrorBody,
  ApiSuccessBody,
  CLIENT_ERROR_CODES,
} from "./types";

const BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1";

/**
 * 인증 연동 지점 — 구현체는 features/auth/token-manager가 등록한다 (US-AUTH-03).
 * - accessToken 공급자: 등록돼 있으면 모든 요청에 Bearer 헤더를 붙인다.
 * - 토큰 갱신 핸들러: `401 TOKEN_EXPIRED` 응답 시 호출된다(스펙 2.5 — 재발급 후 원 요청
 *   재시도). 새 액세스 토큰을 반환하면 원 요청을 **1회만** 재시도한다. single-flight
 *   보장은 핸들러 구현 쪽 책임이다.
 * - 401 핸들러: 재발급으로 복구되지 않는 401에서 호출된다 (세션 정리용).
 */
let accessTokenProvider: (() => string | null) | null = null;
let tokenRefreshHandler: (() => Promise<string | null>) | null = null;
let unauthorizedHandler: ((error: ApiError) => void) | null = null;

export function setAccessTokenProvider(provider: (() => string | null) | null) {
  accessTokenProvider = provider;
}

export function setTokenRefreshHandler(
  handler: (() => Promise<string | null>) | null,
) {
  tokenRefreshHandler = handler;
}

export function setUnauthorizedHandler(
  handler: ((error: ApiError) => void) | null,
) {
  unauthorizedHandler = handler;
}

type QueryParams = Record<
  string,
  string | number | boolean | null | undefined
>;

interface RequestOptions {
  query?: QueryParams;
  /** JSON 직렬화할 본문. FormData면 그대로 전송한다 (엑셀 업로드 등 multipart) */
  body?: unknown;
  headers?: Record<string, string>;
  signal?: AbortSignal;
  /**
   * 익명 API(로그인·재발급) — Authorization 헤더를 붙이지 않는다.
   * 만료된 Bearer를 익명 요청에 실어 보내면 서버 필터가 그걸 먼저 검증해
   * TOKEN_EXPIRED를 낼 수 있고, 재발급 요청이 자기 자신의 재발급을 기다리는
   * 교착으로 이어질 수 있다.
   */
  anonymous?: boolean;
  /** TOKEN_EXPIRED 자동 재발급·재시도를 끈다 — 본문에 리프레시 토큰을 담는 요청(로그아웃)은
   * 재시도 시점에 이미 회전된 옛 토큰을 다시 보내게 되므로 호출부가 직접 처리한다 */
  skipAuthRetry?: boolean;
}

function buildUrl(path: string, query?: QueryParams): string {
  const url = BASE_URL + path;
  if (!query) return url;
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== null && value !== undefined) params.set(key, String(value));
  }
  const qs = params.toString();
  return qs ? `${url}?${qs}` : url;
}

async function request<T>(
  method: string,
  path: string,
  options: RequestOptions = {},
  isRetryAfterRefresh = false,
): Promise<T> {
  const { query, body, signal } = options;
  // 호출자가 넘긴 headers 객체를 변형하지 않도록 복사본에만 쓴다
  const headers: Record<string, string> = { ...options.headers };

  const token = options.anonymous ? null : (accessTokenProvider?.() ?? null);
  if (token) headers["Authorization"] = `Bearer ${token}`;

  let requestBody: BodyInit | undefined;
  if (body instanceof FormData) {
    requestBody = body; // Content-Type은 브라우저가 boundary 포함해 설정
  } else if (body !== undefined) {
    headers["Content-Type"] = "application/json; charset=UTF-8";
    requestBody = JSON.stringify(body);
  }

  let response: Response;
  try {
    response = await fetch(buildUrl(path, query), {
      method,
      headers,
      body: requestBody,
      signal,
    });
  } catch (cause) {
    if (cause instanceof DOMException && cause.name === "AbortError") throw cause;
    throw new ApiError(
      0,
      CLIENT_ERROR_CODES.NETWORK_ERROR,
      "서버에 연결할 수 없습니다. 네트워크 상태를 확인해 주세요.",
    );
  }

  // 실패는 본문 파싱 성패와 무관하게 상태 코드 기준으로 처리한다
  // (프록시가 비 JSON 401을 반환해도 unauthorizedHandler가 호출되도록)
  if (!response.ok) {
    let errorBody: Partial<ApiErrorBody> = {};
    try {
      errorBody = await response.json();
    } catch {
      // 비 JSON 오류 응답 — 공통 코드로 폴백
    }
    const error = new ApiError(
      response.status,
      errorBody.error?.code ?? CLIENT_ERROR_CODES.UNEXPECTED_RESPONSE,
      errorBody.error?.message ?? "요청 처리 중 오류가 발생했습니다.",
      errorBody.error?.details,
    );
    // TOKEN_EXPIRED → 재발급 → 원 요청 1회 재시도 (스펙 2.5). 재시도의 재시도는 없다 —
    // 새 토큰으로도 만료가 나온다면 반복해도 결과가 같다.
    if (
      response.status === 401 &&
      error.code === API_ERROR_CODES.TOKEN_EXPIRED &&
      tokenRefreshHandler &&
      !isRetryAfterRefresh &&
      !options.skipAuthRetry
    ) {
      const renewedToken = await tokenRefreshHandler();
      if (renewedToken !== null) return request<T>(method, path, options, true);
      // 재발급 실패의 후처리는 핸들러가 이미 했다 — 401(세션 종료)과 일시적
      // 네트워크 오류(세션 유지)를 핸들러만 구분할 수 있으므로 여기서
      // unauthorizedHandler로 세션을 또 정리하지 않는다.
      throw error;
    }
    if (response.status === 401) unauthorizedHandler?.(error);
    throw error;
  }

  // 스펙상 본문 없는 성공은 204지만, 프록시가 본문을 제거한 2xx도 실패로 만들지 않는다
  const text = await response.text();
  if (response.status === 204 || text === "") return undefined as T;

  let json: unknown;
  try {
    json = JSON.parse(text);
  } catch {
    json = null;
  }
  if (json === null || typeof json !== "object" || !("data" in json)) {
    throw new ApiError(
      response.status,
      CLIENT_ERROR_CODES.UNEXPECTED_RESPONSE,
      "서버 응답을 해석할 수 없습니다.",
    );
  }

  return (json as ApiSuccessBody<T>).data;
}

/** 도메인 훅 레이어에서만 사용한다 — 컴포넌트에서 직접 호출 금지 (CLAUDE.md) */
export const api = {
  get: <T>(path: string, options?: RequestOptions) =>
    request<T>("GET", path, options),
  post: <T>(path: string, options?: RequestOptions) =>
    request<T>("POST", path, options),
  put: <T>(path: string, options?: RequestOptions) =>
    request<T>("PUT", path, options),
  patch: <T>(path: string, options?: RequestOptions) =>
    request<T>("PATCH", path, options),
  delete: <T = void>(path: string, options?: RequestOptions) =>
    request<T>("DELETE", path, options),
};
