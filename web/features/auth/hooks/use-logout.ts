"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { logoutApi } from "../api/auth-api";
import { useAuth } from "../auth-context";
import { getStoredRefreshToken } from "../token-manager";

/** 로그아웃 (스펙 2.4.5): 서버 무효화가 실패해도 로컬 세션은 반드시 정리한다 */
export function useLogout() {
  const { signOut } = useAuth();
  const router = useRouter();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async () => {
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
