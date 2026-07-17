import {
  ApiError,
  ApiErrorBody,
  ApiSuccessBody,
  CLIENT_ERROR_CODES,
} from "./types";

const BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1";

/**
 * 인증 연동 지점 (US-AUTH-03에서 구현).
 * - accessToken 공급자: 등록돼 있으면 모든 요청에 Bearer 헤더를 붙인다.
 * - 401 핸들러: 응답이 401일 때 호출된다. AUTH 단계에서 TOKEN_EXPIRED 재발급 →
 *   원 요청 재시도(single-flight)로 교체될 뼈대. 지금은 등록된 콜백 호출 후 에러를 그대로 던진다.
 */
let accessTokenProvider: (() => string | null) | null = null;
let unauthorizedHandler: ((error: ApiError) => void) | null = null;

export function setAccessTokenProvider(provider: (() => string | null) | null) {
  accessTokenProvider = provider;
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
): Promise<T> {
  const { query, body, headers = {}, signal } = options;

  const token = accessTokenProvider?.() ?? null;
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

  if (response.status === 204) return undefined as T;

  let json: unknown;
  try {
    json = await response.json();
  } catch {
    throw new ApiError(
      response.status,
      CLIENT_ERROR_CODES.UNEXPECTED_RESPONSE,
      "서버 응답을 해석할 수 없습니다.",
    );
  }

  if (!response.ok) {
    const errorBody = json as Partial<ApiErrorBody>;
    const error = new ApiError(
      response.status,
      errorBody.error?.code ?? CLIENT_ERROR_CODES.UNEXPECTED_RESPONSE,
      errorBody.error?.message ?? "요청 처리 중 오류가 발생했습니다.",
      errorBody.error?.details,
    );
    if (response.status === 401) unauthorizedHandler?.(error);
    throw error;
  }

  return (json as ApiSuccessBody<T>).data;
}

/** 도메인 훅 레이어에서만 사용한다 — 컴포넌트에서 직접 호출 금지 (AGENTS.md) */
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
