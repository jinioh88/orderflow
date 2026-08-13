# 로그인 실패 응답이 스펙(401 INVALID_CREDENTIALS)과 다르게 500 INTERNAL_ERROR로 내려옴

- status: 완료
- 요청 세션: app
- 요청일: 2026-08-13
- 대상 API: `POST /api/v1/auth/login`
- 관련 스토리: US-AUTH-03 (로그인)

## 현재 스펙

`server/docs/api/02-auth.md` 2.4.1 / 2.5:

> 에러: `401 INVALID_CREDENTIALS` — 이메일 없음/비밀번호 불일치. **어느 쪽인지 구분하지 않는다** (계정 존재 비노출)

| HTTP | code | 설명 |
|------|------|------|
| 401 | `INVALID_CREDENTIALS` | 로그인 실패 — 이메일/비밀번호 불일치 (원인 비구분) |

## 요청 내용

로컬 기동 서버(2026-08-13 확인)의 실제 응답이 스펙과 다릅니다. **스펙 변경 요청이 아니라, 구현을
스펙에 맞춰 달라는 요청**입니다 (스펙의 401 정의가 옳다고 보며, 앱은 이미 그 기준으로 구현돼 있습니다).

재현 (미등록 계정으로 로그인):

```
$ curl -s -i -X POST http://localhost:8080/api/v1/auth/login \
    -H 'Content-Type: application/json' \
    -d '{"email":"owner@store.com","password":"password123"}'

HTTP/1.1 500
{"error":{"code":"INTERNAL_ERROR","message":"일시적인 오류가 발생했습니다."}}
```

기대: `401` + `{"error":{"code":"INVALID_CREDENTIALS", ...}}`

같은 서버의 다른 AUTH 응답은 계약대로 확인됐습니다 — 공통 에러 봉투 자체는 정상입니다:

| 요청 | 실제 응답 | 스펙 대비 |
|------|-----------|-----------|
| `POST /auth/login` (미등록 계정) | `500 INTERNAL_ERROR` | ❌ `401 INVALID_CREDENTIALS`이어야 함 |
| `POST /auth/login` (`{"email":"not-an-email"}`) | `400 VALIDATION_ERROR` (+`details[]`) | ✅ 일치 |
| `POST /auth/refresh` (`{"refreshToken":"junk"}`) | `401 INVALID_REFRESH_TOKEN` | ✅ 일치 |

유효 계정으로의 로그인 성공 경로는 **아직 확인하지 못했습니다** (아래 사유 참조). 500이
"계정 없음"만의 문제인지, 로그인 경로 전반의 문제인지는 서버 세션에서 확인이 필요합니다.

## 사유

1. **로그인 실패 문구가 사용자에게 잘못 전달된다.** 앱은 스펙 2.5대로 `401 INVALID_CREDENTIALS`
   분기를 구현해 뒀는데, 실제로는 500이 와서 "일시적인 오류가 발생했습니다." 배너가 뜹니다.
   비밀번호를 틀린 점주에게 서버 장애라고 안내하는 셈이라 재시도 대신 이탈로 이어집니다.
   (US-AUTH-03 인수 조건 — 로그인 실패 사유를 사용자가 알 수 있어야 함)
2. **AUTH-A 리뷰 게이트의 잔여 시연이 막혀 있다.** 로그인 성공 이후 흐름(카탈로그 진입,
   `PASSWORD_SETUP_REQUIRED` 강제 이동, 로그아웃, 401 `TOKEN_EXPIRED` 자동 재발급)은 유효한
   계정으로 실제 로그인이 돼야 확인 가능합니다. 시연에 쓸 **시드 계정(가맹점 점주 role)** 이
   있는지, 없다면 생성 경로를 함께 알려주시면 게이트 시연을 마칠 수 있습니다.

앱 쪽 조치는 없습니다 — 스펙 기준 구현·테스트(128건 통과)는 이미 끝나 있고, 서버가 401을
내려주면 별도 수정 없이 정상 문구로 바뀝니다.

## 참고 — CORS (조치 요청 아님)

서버가 CORS 헤더를 주지 않아(`OPTIONS /api/v1/auth/login` → 401, `Access-Control-Allow-*` 없음)
브라우저에서 API 호출이 차단됩니다. 다만 앱의 실 배포 타깃은 iOS/Android로 CORS와 무관하고,
현재 웹 타깃은 개발 환경에 Xcode·Android 툴체인이 없어 **시연용으로만** 쓰는 것이므로
서버 변경이 필요하다고 보지 않습니다. 시연 시에는 앱과 API를 한 오리진으로 합치는 임시
프록시로 우회했습니다. 서버 세션이 웹(Next.js) 쪽 사정으로 어차피 CORS를 열 계획이라면
그때 함께 처리되면 되는 사안입니다.

## 처리 결과 (서버 세션 기록)

- 처리일: 2026-08-13
- 결론: **서버 코드 변경 없음.** 원인은 구현 결함이 아니라 **로컬 DB에 스키마가 적용되지 않은 상태**였다.
  스펙(401)도 앱 구현도 옳았다.

### 원인

로컬 개발 DB(`orderflow`)에 **테이블이 하나도 없었다.**

```
mysql> USE orderflow; SHOW TABLES;
Empty set          ← 테이블 0개
mysql> USE orderflow_test; SHOW TABLES;
hq_stock, product, store, tenant, users   ← 테스트 DB에만 스키마 존재
```

로컬 프로필은 `ddl-auto: none`(수동 스키마 관리, PM 2026-07-17 결정)이라 기동 시 테이블을
만들지 않고, 스키마 미적용 상태로도 서버는 정상 기동한다. 그래서 첫 DB 접근인
`AuthService.login()`의 `findByEmail()`에서 SQL 예외가 나고 → `GlobalExceptionHandler`의
최종 폴백(`Exception` 핸들러)이 이를 `500 INTERNAL_ERROR`로 변환한 것이다.

요청서의 관측 3건이 정확히 이 가설과 맞아떨어진다 — **DB를 건드리지 않는 요청만 정상 응답**했다:

| 요청 | 결과 | DB 접근 |
|------|------|---------|
| `{"email":"not-an-email"}` | 400 VALIDATION_ERROR ✅ | 없음 (컨트롤러 진입 전 Bean Validation) |
| `/auth/refresh` (junk 토큰) | 401 INVALID_REFRESH_TOKEN ✅ | 없음 (Redis 조회만) |
| `/auth/login` (미등록 계정) | 500 ❌ | **있음 — `users` 테이블 조회** |

즉 앱이 관측한 500은 "계정 없음"의 문제도, 로그인 경로만의 문제도 아니라
**모든 DB 경유 API가 동일하게 500이 되는 상태**였다.

### 조치

스키마를 적용하자 **코드 변경·서버 재기동 없이** 같은 서버가 즉시 401을 반환했다:

```
$ docker exec -i orderflow-mysql mysql -uorderflow -porderflow-local orderflow \
    < infra/src/main/resources/db/schema.sql
$ curl -s -i -X POST localhost:8080/api/v1/auth/login ... -d '{"email":"owner@store.com",...}'

HTTP/1.1 401
{"error":{"code":"INVALID_CREDENTIALS","message":"이메일 또는 비밀번호가 올바르지 않습니다."}}
```

### 검증 — 로그인 성공 경로 포함 (요청서에서 미확인으로 남았던 부분)

시드 데이터 생성 후 AUTH 전 구간을 실제 호출로 확인했다:

| 시나리오 | 결과 |
|----------|------|
| 점주 로그인 (비밀번호 확정 계정) | `200` / `passwordSetupRequired=false` / `role=STORE_OWNER` |
| 점주 로그인 (임시 비밀번호 계정) | `200` / `passwordSetupRequired=true` |
| 비밀번호 불일치 | `401 INVALID_CREDENTIALS` |
| 미등록 이메일 (원 신고 케이스) | `401 INVALID_CREDENTIALS` |
| 카탈로그 조회 (액세스 토큰) | `200` / 3건 |
| 리프레시 회전 | `200` / 새 토큰 쌍 발급 |
| 회전된 옛 리프레시 재사용 | `401 INVALID_REFRESH_TOKEN` |
| 로그아웃 (인증 포함) / 재호출 | `204` / `204` (멱등) |

### 앱 세션에 요청 — 재현 시 확인할 것

앱 쪽 조치는 없다. 다만 **다시 500이 보이면 스펙 위반이 아니라 로컬 환경 문제일 가능성이 높다.**
`docker compose up -d` 이후 **스키마 적용이 별도 단계로 필요**하다 (자동이 아니다).
아래 시드 스크립트가 그 절차와 계정 생성을 한 번에 처리한다.

### 시연용 시드 계정 (요청서 "사유 2" 회신)

`server/scripts/seed-local.sh` 를 추가했다. 실제 API만 호출해
테넌트 → 본사 관리자 → 가맹점 → 점주 계정 → 표본 품목까지 생성한다.

```bash
# 1. 스키마 적용 (기존 로컬 데이터 삭제됨)
docker exec -i orderflow-mysql mysql -uorderflow -porderflow-local orderflow \
  < server/infra/src/main/resources/db/schema.sql
# 2. SYSTEM 계정 부트스트랩 환경변수와 함께 기동
SYSTEM_ADMIN_EMAIL=... SYSTEM_ADMIN_PASSWORD=... ./gradlew :api:bootRun
# 3. 시드
SYSTEM_ADMIN_EMAIL=... SYSTEM_ADMIN_PASSWORD=... SEED_PASSWORD=... ./scripts/seed-local.sh
```

생성되는 계정 (이메일·비밀번호는 환경변수로 조정 가능):

| 역할 | 이메일 | 비밀번호 | 용도 |
|------|--------|----------|------|
| HQ_ADMIN | `hq-admin@orderflow.local` | `SEED_PASSWORD` 값 | 웹 본사 관리자 화면 |
| STORE_OWNER | `owner@orderflow.local` | `SEED_PASSWORD` 값 | **앱 정상 로그인·카탈로그 시연** |
| STORE_OWNER | `owner-temp@orderflow.local` | 실행 시 출력되는 임시 비밀번호 | **`passwordSetupRequired=true` 강제 이동 시연** |

카탈로그 표본 3건(한정 품목 1건, 가용 재고 50)도 함께 생성되므로 목록 화면이 비지 않는다.
자격 증명은 전부 환경변수로 받고 스크립트에 기본값을 두지 않았다 (NFR-2.5).

### CORS (요청서 "참고" 절)

조치하지 않았다 — 요청서가 조치 불요로 명시했고, 앱 실 타깃(iOS/Android)과 무관하다는 판단에
동의한다. 웹(Next.js) 쪽에서 필요해지면 그때 개발 프로파일 한정으로 함께 연다.
