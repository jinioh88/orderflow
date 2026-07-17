import type { Metadata } from "next";

export const metadata: Metadata = { title: "가맹점 관리" };

// US-AUTH-02에서 features/stores의 목록 화면으로 교체될 placeholder
export default function StoresPage() {
  return (
    <div>
      <h1 className="text-xl font-semibold">가맹점 관리</h1>
      <p className="mt-2 text-sm text-gray-500">
        가맹점/계정 관리 화면은 US-AUTH-02에서 구현됩니다.
      </p>
    </div>
  );
}
