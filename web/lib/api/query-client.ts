import { QueryClient } from "@tanstack/react-query";
import { ApiError } from "./types";

/**
 * 4xx는 클라이언트 잘못이므로 재시도하지 않는다. 5xx·네트워크 오류(status 0)만
 * 최대 2회 재시도. 401은 별도 인터셉터가 담당하므로 여기서도 재시도 대상이 아니다.
 */
function shouldRetry(failureCount: number, error: unknown): boolean {
  if (error instanceof ApiError && error.status > 0 && error.status < 500) {
    return false;
  }
  return failureCount < 2;
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
