import { QueryClient } from "@tanstack/react-query";
import { ApiError, CLIENT_ERROR_CODES } from "./types";

/**
 * 5xx와 네트워크 단절만 최대 2회 재시도한다. 4xx는 다시 보내도 같은 결과이고,
 * 401 재발급은 인터셉터 소관이며, AbortError 등 ApiError가 아닌 실패(취소된 요청)를
 * 재시도하면 늦게 도착한 응답이 새 쿼리 결과를 덮을 수 있으므로 전부 제외한다.
 */
function shouldRetry(failureCount: number, error: unknown): boolean {
  if (!(error instanceof ApiError)) return false;
  const retryable =
    error.status >= 500 || error.code === CLIENT_ERROR_CODES.NETWORK_ERROR;
  return retryable && failureCount < 2;
}

export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        retry: shouldRetry,
      },
      mutations: {
        retry: false,
      },
    },
  });
}
