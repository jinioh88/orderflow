# features/ — 도메인별 View/로직 분리 구조

AGENTS.md의 View/로직 분리 원칙에 따라 도메인 코드는 여기에 둔다.

```
features/<도메인>/
  api/         # api-spec 기반 요청 함수 — lib/api/client의 `api`만 사용
  hooks/       # React Query 훅 (useQuery/useMutation) — 컴포넌트가 소비하는 유일한 창구
  components/  # View — 훅이 주는 값·콜백만 소비, fetch/비즈니스 판단 금지
  types.ts     # 도메인 DTO 타입 (api-spec 응답 형태 기준)
```

- `app/` 라우트의 page는 조립만 한다 — 실제 화면은 features의 컴포넌트를 가져다 쓴다.
- 예정 도메인: `auth`(US-AUTH-01~04) · `stores`(US-AUTH-02) · `catalog`(US-CAT-01~04)
