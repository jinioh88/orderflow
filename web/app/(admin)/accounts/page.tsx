import type { Metadata } from "next";
import { AccountsView } from "@/features/accounts/components/accounts-view";

export const metadata: Metadata = { title: "계정 관리" };

export default function AccountsPage() {
  return <AccountsView />;
}
