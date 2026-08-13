"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useToast } from "@/components/ui/toast";
import { userMessageOf } from "@/lib/api/error-messages";
import {
  createAccountApi,
  deactivateAccountApi,
  reissueTemporaryPasswordApi,
} from "../api/accounts-api";
import { accountsKeys } from "./use-accounts";

/**
 * 점주 계정 등록 (US-AUTH-02). 성공 시 임시 비밀번호 1회 표시는 호출부(모달)가
 * 응답의 temporaryPassword로 처리한다 — 토스트에 싣지 않는다(중요 정보, 02 §4.1).
 */
export function useCreateAccount() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createAccountApi,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: accountsKeys.all });
    },
  });
}

/** 임시 비밀번호 재발급 (스펙 2.4.10) — 대상자 세션 전체 무효화 + 임시 상태 전환 */
export function useReissueTemporaryPassword() {
  const queryClient = useQueryClient();
  const toast = useToast();

  return useMutation({
    mutationFn: reissueTemporaryPasswordApi,
    onSuccess: () => {
      // passwordSetupRequired가 true로 바뀌므로 목록 갱신
      queryClient.invalidateQueries({ queryKey: accountsKeys.all });
    },
    onError: (error) => {
      toast.show({ variant: "error", message: userMessageOf(error) });
    },
  });
}

/** 계정 비활성화 (스펙 2.4.12) — 로그인·재발급 즉시 차단 */
export function useDeactivateAccount() {
  const queryClient = useQueryClient();
  const toast = useToast();

  return useMutation({
    mutationFn: deactivateAccountApi,
    onSuccess: (account) => {
      queryClient.invalidateQueries({ queryKey: accountsKeys.all });
      toast.show({
        variant: "success",
        message: `계정 '${account.name}'이(가) 비활성화되었습니다.`,
      });
    },
    onError: (error) => {
      toast.show({ variant: "error", message: userMessageOf(error) });
    },
  });
}
