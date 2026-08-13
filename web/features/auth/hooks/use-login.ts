"use client";

import { useMutation } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api/types";
import { homePathFor, visibleNavItemsFor } from "@/lib/design/nav";
import { loginApi, logoutApi } from "../api/auth-api";
import { useAuth } from "../auth-context";
import { AUTH_CLIENT_ERROR_CODES } from "../types";
import type { LoginRequest } from "../types";

/**
 * 로그인 (US-AUTH-03): 성공 시 세션 시작 후 홈으로, 임시 상태면 비밀번호 설정으로.
 * 웹 메뉴가 없는 역할(점주 등)은 **로그인 단계에서 차단**한다 (사용자 결정 2026-08-13)
 * — 자격 증명이 맞아 서버가 토큰을 발급해도 세션을 만들지 않고 폼 에러로 안내한다.
 */
export function useLogin() {
  const { signIn } = useAuth();
  const router = useRouter();

  return useMutation({
    mutationFn: async (body: LoginRequest) => {
      const result = await loginApi(body);
      if (visibleNavItemsFor(result.user.role).length === 0) {
        // 발급된 리프레시 토큰은 즉시 무효화한다 — 실패해도 세션을 안 만들었으니 무해 (2.4.5 멱등)
        logoutApi(result.refreshToken, result.accessToken).catch(() => {});
        throw new ApiError(
          403,
          AUTH_CLIENT_ERROR_CODES.WEB_ACCESS_DENIED,
          "웹 관리자 화면을 사용할 수 없는 계정입니다.",
        );
      }
      return result;
    },
    onSuccess: (result) => {
      signIn(result);
      router.replace(
        result.passwordSetupRequired
          ? "/password-setup"
          : homePathFor(result.user.role),
      );
    },
  });
}
