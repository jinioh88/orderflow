<!-- BEGIN:nextjs-agent-rules -->
# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` before writing any code. Heed deprecation notices.
<!-- END:nextjs-agent-rules -->

## 구현 원칙 — View / 로직 분리 (PM 지시 2026-07-17)

**UI 컴포넌트(View)와 비즈니스 데이터 로직(Hook/State)을 명확히 분리하여 구조화한다.**

- 컴포넌트는 렌더링과 사용자 입력 전달만 담당한다. API 호출·상태 전이·비즈니스 판단을
  컴포넌트 안에 두지 않는다.
- 데이터 페칭·뮤테이션·파생 상태는 커스텀 훅(예: `features/<도메인>/hooks/`)으로 추출하고,
  컴포넌트는 훅이 반환하는 값·콜백만 소비한다.
- 컴포넌트에서 fetch/API 클라이언트를 직접 호출하지 않는다 — 반드시 훅 레이어를 거친다.
