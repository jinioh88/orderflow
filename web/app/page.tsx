import { redirect } from "next/navigation";

export default function RootPage() {
  // 인증 가드 도입(US-AUTH-03) 전까지는 무조건 로그인으로 보낸다
  redirect("/login");
}
