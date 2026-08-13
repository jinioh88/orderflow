"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { logoutApi } from "../api/auth-api";
import { useAuth } from "../auth-context";
import { decodeJwtClaims } from "../jwt";
import {
  getAccessToken,
  getStoredRefreshToken,
  refreshSession,
} from "../token-manager";

/**
 * 로그아웃 (스펙 2.4.5): 서버 무효화가 실패해도 로컬 세션은 반드시 정리한다.
 * 액세스 토큰이 만료 상태면 먼저 재발급한다 — 만료 상태로 호출해 인터셉터가
 * 재발급·재시도하면 회전 전의 옛 토큰이 본문에 실려 새 토큰이 서버에 살아남는다.
 */
export function useLogout() {
  const { signOut } = useAuth();
  const router = useRouter();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async () => {
      if (!getStoredRefreshToken()) return;
      const claims = decodeJwtClaims(getAccessToken() ?? "");
      const aboutToExpire =
        !claims || claims.exp * 1000 < Date.now() + 30_000;
      // 재발급이 401로 실패하면 세션은 서버·클라이언트 모두 이미 끝난 상태다
      if (aboutToExpire) await refreshSession();
      const refreshToken = getStoredRefreshToken();
      if (!refreshToken) return;
      try {
        await logoutApi(refreshToken);
      } catch {
        // 네트워크 단절 등 — 서버는 못 지웠어도 클라이언트 세션은 끝낸다 (2.4.5 멱등)
      }
    },
    onSettled: () => {
      signOut();
      queryClient.clear(); // 이전 세션의 캐시가 다음 로그인 계정에 보이지 않게
      router.replace("/login");
    },
  });
}
