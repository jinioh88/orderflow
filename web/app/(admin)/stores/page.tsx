import type { Metadata } from "next";
import { StoresView } from "@/features/stores/components/stores-view";

export const metadata: Metadata = { title: "가맹점" };

export default function StoresPage() {
  return <StoresView />;
}
