"use client";

import { useAuth } from "../auth-context";

/**
 * 사이드바 상단 로고 영역의 테넌트명 (03 §1, 스펙 2.4.2 tenant.name).
 * 테넌트명이 없으면(복원 전·구버전 스냅샷) 서비스명으로 폴백한다.
 */
export function SidebarTenant() {
  const { tenantName } = useAuth();
  return <>{tenantName ?? "OrderFlow"}</>;
}
