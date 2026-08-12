import {
  BarChart3,
  ClipboardList,
  FileWarning,
  Store,
  Tag,
  Truck,
  Users,
  Wallet,
  type LucideIcon,
} from "lucide-react";
import type { Role } from "@/features/auth/types";

/**
 * 사이드바 메뉴 (03-web-components §1). 아이콘은 01-foundations §4의 도메인 매핑 표를 따른다.
 *
 * **미구현 마일스톤의 메뉴는 숨긴다** — disabled로 두지 않는다(데모에서 미완성처럼 보임).
 * 화면이 실제로 생기면 해당 항목의 `enabled`를 켜는 방식으로 관리한다.
 *
 * `roles`는 해당 화면 목록 API의 접근 역할(스펙 각 에픽의 권한 표)을 따른다 — 역할에 없는
 * 메뉴는 노출하지 않고, 라우트 직접 진입도 가드가 홈으로 돌려보낸다 (US-AUTH-03).
 */
export interface NavItem {
  href: string;
  label: string;
  icon: LucideIcon;
  milestone: string;
  /** false면 사이드바에 나타나지 않는다 */
  enabled: boolean;
  /** 이 메뉴를 볼 수 있는 역할 — 근거 스펙 절은 각 항목 주석 참조 */
  roles: Role[];
}

const HQ_ROLES: Role[] = ["HQ_ADMIN", "HQ_MANAGER"];

export const NAV_ITEMS: NavItem[] = [
  // M2+ 항목의 roles는 화면 착수 시 해당 에픽 스펙의 권한 표로 확정한다 — 지금은 본사 공통
  { href: "/dashboard", label: "대시보드", icon: BarChart3, milestone: "M2", enabled: false, roles: HQ_ROLES },
  { href: "/orders", label: "수주 관리", icon: ClipboardList, milestone: "M3", enabled: false, roles: HQ_ROLES },
  { href: "/shipments", label: "출하 지시", icon: Truck, milestone: "M2~3", enabled: false, roles: HQ_ROLES },
  // GET /products는 테넌트 소속 전체 허용(3.2.2)이지만 웹은 본사 도구이므로 본사 역할에만 노출
  { href: "/products", label: "상품", icon: Tag, milestone: "M1", enabled: true, roles: HQ_ROLES },
  { href: "/claims", label: "클레임", icon: FileWarning, milestone: "M4", enabled: false, roles: HQ_ROLES },
  { href: "/settlements", label: "정산·미수금", icon: Wallet, milestone: "M4", enabled: false, roles: HQ_ROLES },
  // GET /stores — HQ_ADMIN, HQ_MANAGER (2.4.7)
  { href: "/stores", label: "가맹점", icon: Store, milestone: "M1", enabled: true, roles: HQ_ROLES },
  // 별도 메뉴로 확정 (사용자 결정 2026-08-12) — GET /users (2.4.11)
  { href: "/accounts", label: "계정 관리", icon: Users, milestone: "M1", enabled: true, roles: HQ_ROLES },
];

export function visibleNavItemsFor(role: Role | null): NavItem[] {
  if (!role) return [];
  return NAV_ITEMS.filter((item) => item.enabled && item.roles.includes(role));
}

/** 로그인 성공 후 도착지 — 역할이 볼 수 있는 첫 메뉴 (US-AUTH-03 "관리자 홈") */
export function homePathFor(role: Role | null): string {
  return visibleNavItemsFor(role)[0]?.href ?? "/products";
}

function matches(item: NavItem, pathname: string): boolean {
  return pathname === item.href || pathname.startsWith(`${item.href}/`);
}

/** 현재 경로의 메뉴 항목 — 라우트 접근 제어용 */
export function navItemOf(pathname: string): NavItem | undefined {
  return NAV_ITEMS.find((item) => matches(item, pathname));
}

/** 현재 경로에 해당하는 메뉴 라벨 — 헤더의 페이지 타이틀로 쓴다 */
export function navLabelOf(pathname: string): string | undefined {
  return navItemOf(pathname)?.label;
}
