# 로그인 응답에 테넌트 이름 추가 (사이드바 테넌트명 표시)

- status: 완료
- 요청 세션: web
- 요청일: 2026-08-12
- 대상 API: POST /auth/login (스펙 2.4.2)
- 관련 스토리: US-AUTH-03

## 현재 스펙

로그인 응답의 `user`: `{ id, email, name, role, tenantId, storeId }` — 테넌트의 **id만** 있고
이름이 없다. 테넌트 이름을 조회할 다른 API도 웹에 허용된 범위에는 없다
(`POST /system/tenants`는 SYSTEM 전용 등록 API).

## 요청 내용

로그인 응답 `data`에 테넌트 이름을 추가해 달라. 제안:

```json
{
  "data": {
    "accessToken": "...",
    "user": { ... },
    "tenant": { "id": 1, "name": "본죽F&B" }
  }
}
```

또는 최소 형태로 `user.tenantName` 필드 하나. SYSTEM 로그인은 `null`이면 된다.
기존 필드는 그대로 두는 추가 변경이라 하위 호환을 깨지 않는다.

## 사유

디자인 시스템 03-web-components §1 앱 셸 스펙이 사이드바 상단 로고 영역(h=56)에
**테넌트명 표시**를 요구한다. 현재 로그인 흐름에서 테넌트 이름을 얻을 방법이 없어
"OrderFlow" 고정 텍스트로 두었다. 반영 전까지 화면 구현은 이대로 진행한다 (차단 아님).

## 처리 결과 (서버 세션 기록)

- 승인: 사용자, 2026-08-13 — **A안(`tenant` 객체) 채택**
- 반영일: 2026-08-13
- 스펙 갱신 위치: `server/docs/api/02-auth.md` 2.4.2 (예시 JSON + 필드 표)

### 반영 내용

로그인 응답 `data`에 `tenant` 객체를 추가했다. **순수 추가 변경으로 기존 필드는 그대로다.**

```json
{
  "data": {
    "accessToken": "...",
    "accessTokenExpiresIn": 1800,
    "refreshToken": "...",
    "passwordSetupRequired": false,
    "user": { "id": 42, "email": "owner@example.com", "name": "박점주",
              "role": "STORE_OWNER", "tenantId": 1, "storeId": 7 },
    "tenant": { "id": 1, "name": "본죽F&B" }
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `tenant.id` | number | 테넌트 ID — `user.tenantId`와 항상 동일 |
| `tenant.name` | string | 테넌트명 (최대 100자) |

**SYSTEM 계정은 `tenant: null`** (요청서 제안대로). 로컬 서버 실제 응답으로 확인:

```
STORE_OWNER 로그인 → "tenant": { "id": 1, "name": "본죽F&B" }
SYSTEM 로그인      → "tenant": null
```

### A안을 고른 이유

`user`는 계정 정보이고 테넌트는 별개 애그리거트다. B안(`user.tenantName`)은 필드 하나로
끝나지만 `UserSummary`가 타 애그리거트 정보를 흡수하기 시작하는 형태라, 이후 테넌트 속성
(로고·마감시각 등)이 더 필요해질 때 같은 자리에 계속 덧붙게 된다. `tenant` 객체로 분리해 두면
그 확장이 자연스럽다.

성능 비용은 없다 — 로그인 가드(`assertLoginAllowed`)가 테넌트 활성 여부 확인을 위해
**이미 Tenant를 조회하고 있었고**, 그 결과를 버리지 않고 응답에 재사용하도록 바꿨을 뿐이다.
조회 횟수는 변경 전과 동일하다.

### 웹 세션 조치

사이드바 로고 영역의 `"OrderFlow"` 고정 텍스트를 `data.tenant.name`으로 교체하면 된다.
SYSTEM 계정으로 로그인한 경우 `tenant`가 `null`이므로 대체 텍스트 처리가 필요하다.

### 테스트

`AuthFlowApiTest`에 2건 추가 (전체 118건 통과):
- 로그인 응답에 소속 테넌트 요약이 포함된다 — `tenant.id`/`tenant.name`, `user.tenantId`와 일치
- SYSTEM 로그인은 `tenant`가 `null`
