# [보안] 위조된 액세스 토큰이 인증을 통과한다 — 아무 문자열이나 `Bearer`로 붙이면 200

- status: 완료 (재현 불가 — 서버 코드 변경 없음, 회귀 테스트 추가)
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

- 처리일: 2026-08-13
- 결론: **서버에서 재현되지 않는다.** 위조 토큰은 전부 `401 UNAUTHORIZED`로 거부된다.
  **토큰 검증 코드는 고친 적이 없다** — 즉 "고쳐서 해결된 것"이 아니라 처음부터 거부하고 있었다.

### 재현 시도 — 요청서의 절차 그대로 (2026-08-13, 로컬 기동 서버)

| 요청 | 요청서 관측 | 이번 실측 |
|------|------------|-----------|
| 헤더 없음 | 401 | `401` |
| `Bearer forged.token.value` | **200** | `401 UNAUTHORIZED` |
| `Bearer x` | **200 + 실데이터** | `401 UNAUTHORIZED` |
| `POST /auth/logout` + `Bearer x` | **204** | `401 UNAUTHORIZED` |

추가로 **가장 현실적인 위조**인 서명 변조도 확인했다 — 정상 토큰의 서명부만 갈아끼운 경우 `401`,
페이로드는 유효하나 다른 키로 서명한 경우도 `401`. 정상 토큰만 `200`.

### 코드 근거 — 필터는 원래부터 검증하고 있었다

`JwtAuthenticationFilter`는 최초 AUTH 구현 커밋(`3da55a3`, US-AUTH-01~04) 이후
**한 번도 수정되지 않았다.** 즉 요청서가 작성된 시점에도 지금과 같은 코드였다.

```java
try {
    principal = tokenProvider.parse(header.substring(BEARER_PREFIX.length()));
} catch (JwtException | IllegalArgumentException e) {
    chain.doFilter(request, response);   // 인증 세팅 없이 통과 → 인가 단계에서 401
    return;
}
```

서명·만료 검증에 실패하면 `SecurityContext`에 인증을 넣지 않고 넘기며,
`SecurityConfig`의 `anyRequest().authenticated()`가 401 EntryPoint로 보낸다.

### 관측된 데이터가 이 서버의 것이 아니다

요청서에 찍힌 응답은 `{"id":391,"productCode":"P-0391","name":"고기만두 391호"}`인데,
**이 데이터는 어디에도 없다** — 로컬 DB(`orderflow`)·테스트 DB(`orderflow_test`) 모두에 없고,
서버 저장소의 어떤 테스트 픽스처 코드에도 `P-03xx`·`~호` 형태의 품명 생성 로직이 없다.
서버가 만든 적 없는 데이터다.

따라서 **그 200 응답은 이 서버가 낸 것이 아닐 가능성이 높다.** 확정할 수는 없지만,
앱 세션이 별건(`20260813-app-login-500-instead-of-401.md`)에서 언급한
"앱과 API를 한 오리진으로 합치는 임시 프록시"가 목 데이터를 돌려준 경우가 가장 그럴듯하다 —
목 서버라면 헤더 값을 검증하지 않고 임의 상품 목록을 주는 동작이 자연스럽다.

**앱 세션에 요청**: 같은 증상이 다시 보이면 ①요청이 실제로 도달한 포트/오리진과
②그 시점 서버 로그를 함께 알려 달라. 위 가설이 틀렸다면 서버가 놓치고 있는 경로가 있다는 뜻이라
다시 파야 한다.

### 조치 — 자동 검증이 비어 있던 것은 사실이라 채웠다

재현은 안 됐지만 **위조 토큰 거부를 검증하는 테스트가 없었다.** 기존에는 "헤더 없음 → 401"만
있었고(`TenantRegistrationApiTest`), 위조·서명 변조 케이스는 비어 있었다. 보안 리포트가 제기된
지점이므로 회귀 테스트로 고정했다 — `AuthFlowApiTest.forgedAccessTokenIsRejected()`:

- JWT 형식이 아닌 문자열 4종 (`x`, `forged.token.value`, `Bearer`, `...`) → 401 UNAUTHORIZED
- 페이로드는 유효하나 **다른 키로 서명**한 토큰 → 401 UNAUTHORIZED
- 정상 토큰의 **서명부만 변조**한 토큰 → 401 UNAUTHORIZED

이제 인증이 뚫리는 변경이 들어오면 테스트가 실패한다. 만료 토큰 → `TOKEN_EXPIRED` 테스트는
기존에 있었고 그대로 통과한다.

### 스펙 관련

요청서가 물은 "의도된 로컬 개발 편의(dev 프로파일에서 검증 생략)"는 **없다.**
프로파일과 무관하게 항상 검증하며, 스펙 1.2·1.4가 규정한 대로 동작한다. 스펙 변경 없음.
