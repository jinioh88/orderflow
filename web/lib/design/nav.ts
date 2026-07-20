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

/**
 * 사이드바 메뉴 (03-web-components §1). 아이콘은 01-foundations §4의 도메인 매핑 표를 따른다.
 *
 * **미구현 마일스톤의 메뉴는 숨긴다** — disabled로 두지 않는다(데모에서 미완성처럼 보임).
 * 화면이 실제로 생기면 해당 항목의 `enabled`를 켜는 방식으로 관리한다.
 */
export interface NavItem {
  href: string;
  label: string;
  icon: LucideIcon;
  milestone: string;
  /** false면 사이드바에 나타나지 않는다 */
  enabled: boolean;
}

export const NAV_ITEMS: NavItem[] = [
  { href: "/dashboard", label: "대시보드", icon: BarChart3, milestone: "M2", enabled: false },
  { href: "/orders", label: "수주 관리", icon: ClipboardList, milestone: "M3", enabled: false },
  { href: "/shipments", label: "출하 지시", icon: Truck, milestone: "M2~3", enabled: false },
  { href: "/products", label: "상품", icon: Tag, milestone: "M1", enabled: true },
  { href: "/claims", label: "클레임", icon: FileWarning, milestone: "M4", enabled: false },
  { href: "/settlements", label: "정산·미수금", icon: Wallet, milestone: "M4", enabled: false },
  { href: "/stores", label: "가맹점", icon: Store, milestone: "M1", enabled: true },
  // 계정 관리 화면은 US-AUTH-02 착수 시 라우트와 함께 켠다
  { href: "/accounts", label: "계정 관리", icon: Users, milestone: "M1", enabled: false },
];

export const VISIBLE_NAV_ITEMS = NAV_ITEMS.filter((item) => item.enabled);

/** 현재 경로에 해당하는 메뉴 라벨 — 헤더의 페이지 타이틀로 쓴다 */
export function navLabelOf(pathname: string): string | undefined {
  return NAV_ITEMS.find(
    (item) => pathname === item.href || pathname.startsWith(`${item.href}/`),
  )?.label;
}
