"use client";

import { QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { useState } from "react";
import { createQueryClient } from "@/lib/api/query-client";

export function Providers({ children }: { children: React.ReactNode }) {
  // useState 초기화 함수로 생성 — 리렌더에도 QueryClient 인스턴스를 유지한다
  const [queryClient] = useState(createQueryClient);

  return (
    <QueryClientProvider client={queryClient}>
      {children}
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  );
}
