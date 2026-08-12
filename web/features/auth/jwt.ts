import type { JwtClaims } from "./types";

/**
 * 액세스 토큰 페이로드 디코딩 — 역할 기반 메뉴/라우트 노출 판단용.
 * 서명은 검증하지 않는다(클라이언트는 키가 없어 할 수도 없다). 위조된 role로 메뉴를
 * 열어 봤자 모든 API가 서버에서 403/404로 막히므로 표시 판단에는 충분하다 (NFR-2.3).
 */
export function decodeJwtClaims(token: string): JwtClaims | null {
  const payload = token.split(".")[1];
  if (!payload) return null;
  try {
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    const utf8 = Uint8Array.from(atob(base64), (c) => c.charCodeAt(0));
    return JSON.parse(new TextDecoder().decode(utf8)) as JwtClaims;
  } catch {
    return null;
  }
}
