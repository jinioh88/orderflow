import type { Metadata } from "next";

export const metadata: Metadata = { title: "상품 관리" };

// 페이지 타이틀은 앱 셸 헤더가 그린다(03 §1) — 페이지는 본문만 담당한다.
// US-CAT-01~04에서 features/catalog의 그리드 화면으로 교체될 placeholder
export default function ProductsPage() {
  return (
    <p className="text-body-md text-fg-caption">
      상품 카탈로그 화면은 US-CAT-01~04에서 구현됩니다.
    </p>
  );
}
