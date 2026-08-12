# 로그인 응답에 테넌트 이름 추가 (사이드바 테넌트명 표시)

- status: 요청됨
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

(미처리)
