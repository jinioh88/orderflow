# [보안] 위조된 액세스 토큰이 인증을 통과한다 — 아무 문자열이나 `Bearer`로 붙이면 200

- status: 요청됨
- 요청 세션: app
- 요청일: 2026-08-13
- 대상 API: 인증이 필요한 전체 (확인한 것: `GET /products`, `POST /auth/logout`)
- 관련 스토리: US-AUTH-03·04, NFR-2.1~2.4

## 현재 스펙

`api-spec.md` 1.2 — 인증이 필요한 모든 API는 `Authorization: Bearer <accessToken>`이며,
토큰은 **JWT(클레임: tenant_id, store_id, role)** 다. 1.4는 "토큰 없음·**위조**"를
`401 UNAUTHORIZED`로 규정한다.

NFR-2.1 / 2.2 / 2.4 — 모든 데이터 접근은 토큰의 테넌트 범위로 강제 격리되고,
교차 테넌트 접근은 404로 비노출돼야 한다.

## 요청 내용

로컬 기동 서버(2026-08-13 확인)가 **서명 검증 없이 아무 문자열이나 액세스 토큰으로 받아들인다.**
스펙 1.2·1.4대로 서명·만료를 검증해 위조 토큰을 `401 UNAUTHORIZED`로 거부해 달라.

재현 — 헤더 유무만 다르고 나머지는 동일하다:

```
$ curl -s -o /dev/null -w '%{http_code}\n' 'localhost:8080/api/v1/products?size=1'
401                                    ← 헤더 없음: 정상

$ curl -s -o /dev/null -w '%{http_code}\n' 'localhost:8080/api/v1/products?size=1' \
    -H 'Authorization: Bearer forged.token.value'
200                                    ← 위조 토큰: 상품 목록이 그대로 응답됨

$ curl -s 'localhost:8080/api/v1/products?limited=true&size=3' -H 'Authorization: Bearer x'
{"data":{"items":[{"id":391,"productCode":"P-0391","name":"고기만두 391호", ...
                                       ← "x" 한 글자로도 실데이터 조회
```

`GET /products`만의 문제가 아니다 — 상태를 바꾸는 API도 같다:

```
$ curl -s -o /dev/null -w '%{http_code}\n' -X POST 'localhost:8080/api/v1/auth/logout' \
    -H 'Authorization: Bearer x' -H 'Content-Type: application/json' -d '{"refreshToken":"x"}'
204
```

즉 현재 서버는 `Authorization` 헤더의 **존재 여부만** 보고 값은 검증하지 않는 것으로 보인다.

## 사유

1. **인증이 사실상 없다.** 아무나 헤더 한 줄로 테넌트 전체 카탈로그를 읽을 수 있다.
2. **테넌트 격리(NFR-2.1·2.4)가 성립할 수 없다.** 격리는 토큰의 `tenant_id` 클레임에서 나오는데,
   검증되지 않은 토큰에는 신뢰할 수 있는 클레임이 없다. 지금 데이터가 반환된다는 것은 서버가
   토큰과 무관한 어떤 기본값으로 테넌트를 정하고 있다는 뜻이고, 그러면 "교차 테넌트 접근 시
   404"(NFR-2.1)를 검증할 방법 자체가 없다.
3. **앱의 토큰 재발급 흐름(2.5)이 조용히 무의미해진다.** 앱은 `401 TOKEN_EXPIRED`를 받아
   재발급하도록 구현돼 있는데, 만료된 토큰도 200이 오면 그 경로가 아예 실행되지 않는다.
   지금은 앱 단위 테스트로만 검증되고 실서버 통합에서는 확인이 불가능하다.

의도된 로컬 개발 편의라면(예: dev 프로파일에서 검증 생략) 그 사실을 스펙이나 README에
명시해 달라 — 앱은 스펙 1.2를 그대로 믿고 구현하고 있어서, 지금 상태로는 **인증 관련 동작을
실서버로 검증했다고 말할 수 없다.**

## 앱 세션 조치

앱 쪽에서 할 수 있는 일은 없다 (토큰 검증은 서버 책임). 이 건으로 작업을 멈추지 않고
CAT 태스크를 계속 진행한다.

## 처리 결과 (서버 세션 기록)

(미작성)
