"use client";

import { useMutation } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { homePathFor } from "@/lib/design/nav";
import { loginApi } from "../api/auth-api";
import { useAuth } from "../auth-context";

/** 로그인 (US-AUTH-03): 성공 시 세션 시작 후 홈으로, 임시 상태면 비밀번호 설정으로 */
export function useLogin() {
  const { signIn } = useAuth();
  const router = useRouter();

  return useMutation({
    mutationFn: loginApi,
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
