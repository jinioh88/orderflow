"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useToast } from "@/components/ui/toast";
import { userMessageOf } from "@/lib/api/error-messages";
import { createStoreApi, deactivateStoreApi } from "../api/stores-api";
import { storesKeys } from "./use-stores";

/** 가맹점 등록 (US-AUTH-02). 폼 에러 매핑은 호출부(모달) 몫 — 여기서는 캐시·토스트만 */
export function useCreateStore() {
  const queryClient = useQueryClient();
  const toast = useToast();

  return useMutation({
    mutationFn: createStoreApi,
    onSuccess: (store) => {
      queryClient.invalidateQueries({ queryKey: storesKeys.all });
      toast.show({
        variant: "success",
        message: `가맹점 '${store.name}'이(가) 등록되었습니다.`,
      });
    },
  });
}

/** 가맹점 비활성화 (스펙 2.4.8) — 소속 사용자 로그인·재발급 즉시 차단 */
export function useDeactivateStore() {
  const queryClient = useQueryClient();
  const toast = useToast();

  return useMutation({
    mutationFn: deactivateStoreApi,
    onSuccess: (store) => {
      queryClient.invalidateQueries({ queryKey: storesKeys.all });
      toast.show({
        variant: "success",
        message: `가맹점 '${store.name}'이(가) 비활성화되었습니다.`,
      });
    },
    onError: (error) => {
      // 409 CONFLICT(이미 비활성) 등 — 확인 다이얼로그가 닫힌 뒤라 토스트로 알린다
      toast.show({ variant: "error", message: userMessageOf(error) });
    },
  });
}
