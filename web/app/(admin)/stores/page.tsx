import type { Metadata } from "next";

export const metadata: Metadata = { title: "가맹점 관리" };

// US-AUTH-02에서 features/stores의 목록 화면으로 교체될 placeholder
export default function StoresPage() {
  return (
    <p className="text-body-md text-fg-caption">
      가맹점/계정 관리 화면은 US-AUTH-02에서 구현됩니다.
    </p>
  );
}
