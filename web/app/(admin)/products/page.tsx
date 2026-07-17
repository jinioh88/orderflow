import type { Metadata } from "next";

export const metadata: Metadata = { title: "상품 관리" };

// US-CAT-01~04에서 features/catalog의 그리드 화면으로 교체될 placeholder
export default function ProductsPage() {
  return (
    <div>
      <h1 className="text-xl font-semibold">상품 관리</h1>
      <p className="mt-2 text-sm text-gray-500">
        상품 카탈로그 화면은 US-CAT-01~04에서 구현됩니다.
      </p>
    </div>
  );
}
