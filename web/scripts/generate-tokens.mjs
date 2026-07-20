/**
 * design/design-system/tokens.json → app/tokens.css 생성기.
 *
 * 토큰의 단일 진실 공급원은 tokens.json(PM 소유)이다. 이 스크립트는 그 값을
 * Tailwind v4의 `@theme` 블록으로 옮겨, 화면 코드가 hex를 직접 쓰지 않게 한다.
 *
 *   npm run tokens
 *
 * 매핑 규칙 (01-foundations §0의 3계층 구조를 그대로 반영):
 * - core  → `:root`에만 `--core-*` 변수로 둔다. @theme에 넣지 않으므로 Tailwind
 *           유틸리티가 생성되지 않는다 = 화면 코드에서 core를 직접 못 쓴다(규칙 강제).
 * - semantic/web → `@theme`에 넣어 유틸리티(bg-*, text-*, rounded-*, shadow-*)를 만든다.
 *
 * 색 변수 이름에 `fg-` 접두사를 쓰는 이유: Tailwind는 `--text-*`를 font-size 유틸로
 * 해석하므로, 텍스트 '색'은 `--color-fg-title`(→ text-fg-title)로 분리해야 충돌하지 않는다.
 */

import { readFileSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const TOKENS_PATH = resolve(here, "../../design/design-system/tokens.json");
const OUT_PATH = resolve(here, "../app/tokens.css");

const tokens = JSON.parse(readFileSync(TOKENS_PATH, "utf8"));

/** `{core.color.blue.600}` 참조를 실제 값으로 해석한다 (중첩 참조도 재귀 해석) */
function resolveRef(value) {
  if (typeof value !== "string") return value;
  const match = /^\{([^}]+)\}$/.exec(value);
  if (!match) return value;
  const resolved = match[1]
    .split(".")
    .reduce((node, key) => (node === undefined ? undefined : node[key]), tokens);
  if (resolved === undefined) {
    throw new Error(`tokens.json: 해석할 수 없는 참조 ${value}`);
  }
  return resolveRef(resolved);
}

/** camelCase → kebab-case (primaryHover → primary-hover) */
const kebab = (s) => s.replace(/([a-z0-9])([A-Z])/g, "$1-$2").toLowerCase();

const lines = [];
const push = (line = "") => lines.push(line);

push("/* 자동 생성 파일 — 직접 수정하지 말 것.");
push(" * 생성: npm run tokens  (원본: design/design-system/tokens.json)");
push(` * tokens.json 버전: ${tokens.$meta.version}`);
push(" */");
push();

// ── core: 원시 팔레트. 유틸리티를 만들지 않으려고 @theme 바깥에 둔다 ──────────
push("/* core — 원시 팔레트. 화면 코드에서 직접 참조 금지(01-foundations §0).");
push("   semantic/web 토큰의 원본 값 추적용으로만 노출한다. */");
push(":root {");
for (const [family, scale] of Object.entries(tokens.core.color)) {
  for (const [step, hex] of Object.entries(scale)) {
    push(`  --core-color-${family}-${step}: ${hex};`);
  }
}
push("}");
push();

push("@theme {");

// ── 폰트 ────────────────────────────────────────────────────────────────────
push("  /* 폰트 — 실제 Pretendard 로드는 next/font/local(app/fonts.ts)이 담당하고,");
push("     여기서는 폴백 체인만 정의한다. */");
push(
  `  --font-sans: var(--font-pretendard), ${tokens.core.fontFamily.fallbackWeb};`,
);
push();

// ── 색: web 플랫폼 토큰 ──────────────────────────────────────────────────────
const FG_KEYS = {
  textTitle: "fg-title",
  textBody: "fg-body",
  textCaption: "fg-caption",
  textDisabled: "fg-disabled",
};
push("  /* web.color — 브랜드·표면·텍스트 */");
for (const [key, value] of Object.entries(tokens.web.color)) {
  const name = FG_KEYS[key] ?? kebab(key);
  push(`  --color-${name}: ${resolveRef(value)};`);
}
push();

// ── 색: semantic 피드백 4색 ─────────────────────────────────────────────────
push("  /* semantic.feedback — 성공·주의·위험·안내 (웹·앱 공통 hue) */");
for (const [name, parts] of Object.entries(tokens.semantic.color.feedback)) {
  for (const [part, value] of Object.entries(parts)) {
    push(`  --color-${name}-${part}: ${resolveRef(value)};`);
  }
}
push();

// ── 색: 발주 상태 7색 ───────────────────────────────────────────────────────
push("  /* semantic.orderStatus — 발주 상태 7색. 라벨은 lib/design/order-status.ts */");
for (const [status, parts] of Object.entries(tokens.semantic.color.orderStatus)) {
  const key = status.toLowerCase().replace(/_/g, "-");
  for (const part of ["dot", "text", "bg"]) {
    push(`  --color-status-${key}-${part}: ${resolveRef(parts[part])};`);
  }
}
push();

// ── 타이포그래피 ────────────────────────────────────────────────────────────
push("  /* web.typography — 크기/행간/굵기를 한 유틸리티(text-*)로 묶는다 */");
for (const [name, spec] of Object.entries(tokens.web.typography)) {
  const key = kebab(name);
  push(`  --text-${key}: ${spec.size}px;`);
  push(`  --text-${key}--line-height: ${spec.lineHeight}px;`);
  push(`  --text-${key}--font-weight: ${spec.weight};`);
}
push();

// ── 라운드 · 그림자 · 레이아웃 ──────────────────────────────────────────────
push("  /* web.radius — Tailwind 기본값을 웹 스케일로 덮어쓴다 */");
for (const [name, value] of Object.entries(tokens.web.radius)) {
  push(`  --radius-${name}: ${value}px;`);
}
push();

push("  /* web.shadow — 관제탑은 보더 우선, 카드는 shadow-1이 상한(01 §3.3) */");
for (const [name, value] of Object.entries(tokens.web.shadow)) {
  push(`  --shadow-${name}: ${value};`);
}
push();

push("  /* web.layout — 셸 치수(03 §1). w-sidebar / h-header 로 쓴다 */");
push(`  --spacing-sidebar: ${tokens.web.layout.sidebarWidth}px;`);
push(`  --spacing-header: ${tokens.web.layout.headerHeight}px;`);
push(`  --spacing-min-viewport: ${tokens.web.layout.minViewport}px;`);
push("}");
push();

writeFileSync(OUT_PATH, lines.join("\n"), "utf8");
console.log(`generated ${OUT_PATH} (tokens.json v${tokens.$meta.version})`);
