"use client";

import { useMutation } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { homePathFor } from "@/lib/design/nav";
import { setPasswordApi } from "../api/auth-api";
import { useAuth } from "../auth-context";

/** 최초 로그인 비밀번호 설정 (US-AUTH-02·03, 스펙 2.4.4): 성공 시 새 토큰 쌍 반영 후 홈 진입 */
export function useSetPassword() {
  const { role, completePasswordSetup } = useAuth();
  const router = useRouter();

  return useMutation({
    mutationFn: setPasswordApi,
    onSuccess: (result) => {
      completePasswordSetup(result);
      router.replace(homePathFor(role));
    },
  });
}
