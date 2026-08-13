#!/usr/bin/env bash
# ============================================================
# 로컬 시연용 시드 데이터 생성 — 테넌트 → 가맹점 → 점주 계정
#
# 실제 API만 호출한다 (SQL 직접 삽입 없음). 그래서 이 스크립트가 도는 것 자체가
# US-AUTH-01/02/03 경로가 살아 있다는 증명이 된다.
#
# 사전 조건
#   1. docker compose up -d  (MySQL + Redis)
#   2. 스키마 적용 — 로컬 DB는 ddl-auto: none 이라 자동 생성되지 않는다:
#        docker exec -i orderflow-mysql mysql -uorderflow -porderflow-local orderflow \
#          < infra/src/main/resources/db/schema.sql
#   3. SYSTEM 계정 부트스트랩 환경변수를 준 채로 서버 기동:
#        SYSTEM_ADMIN_EMAIL=... SYSTEM_ADMIN_PASSWORD=... ./gradlew :api:bootRun
#
# 실행
#   SYSTEM_ADMIN_EMAIL=... SYSTEM_ADMIN_PASSWORD=... SEED_PASSWORD=... \
#     ./scripts/seed-local.sh
#
# 자격 증명은 전부 환경변수로 받는다 — 스크립트에 기본값을 두지 않는다 (NFR-2.5).
# 재실행 시 이메일 중복(409 EMAIL_DUPLICATED)으로 실패한다. 초기화하려면 스키마를 다시 적용한다.
# ============================================================
set -euo pipefail

API="${API_BASE:-http://localhost:8080}"
: "${SYSTEM_ADMIN_EMAIL:?SYSTEM_ADMIN_EMAIL 환경변수가 필요하다 (서버 기동 시 쓴 값과 동일해야 함)}"
: "${SYSTEM_ADMIN_PASSWORD:?SYSTEM_ADMIN_PASSWORD 환경변수가 필요하다}"
: "${SEED_PASSWORD:?SEED_PASSWORD 환경변수가 필요하다 — 시연 계정이 쓸 비밀번호 (8~64자, 영문자+숫자)}"

TENANT_NAME="${SEED_TENANT_NAME:-본죽F&B}"
STORE_NAME="${SEED_STORE_NAME:-강남점}"
HQ_EMAIL="${SEED_HQ_EMAIL:-hq-admin@orderflow.local}"
OWNER_EMAIL="${SEED_OWNER_EMAIL:-owner@orderflow.local}"
OWNER_TEMP_EMAIL="${SEED_OWNER_TEMP_EMAIL:-owner-temp@orderflow.local}"

command -v jq >/dev/null || { echo "jq가 필요하다 — brew install jq" >&2; exit 1; }

# API 호출 공통 — 2xx가 아니면 응답 본문을 그대로 보여주고 중단한다.
# (스펙 1.3 에러 봉투가 그대로 나오므로 원인 파악이 바로 된다)
call() {
  local method=$1 path=$2 body=${3:-} token=${4:-}
  local args=(-s -w '\n%{http_code}' -X "$method" "$API$path" -H 'Content-Type: application/json')
  [[ -n $token ]] && args+=(-H "Authorization: Bearer $token")
  [[ -n $body ]] && args+=(-d "$body")

  local out status
  out=$(curl "${args[@]}")
  status=$(tail -n1 <<<"$out")
  out=$(sed '$d' <<<"$out")
  if [[ $status != 2* ]]; then
    echo "✗ $method $path → HTTP $status" >&2
    echo "$out" >&2
    exit 1
  fi
  echo "$out"
}

login() { # email password → accessToken
  call POST /api/v1/auth/login "$(jq -nc --arg e "$1" --arg p "$2" '{email:$e,password:$p}')" \
    | jq -r '.data.accessToken'
}

echo "▶ 1/5 SYSTEM 로그인 ($SYSTEM_ADMIN_EMAIL)"
SYSTEM_TOKEN=$(login "$SYSTEM_ADMIN_EMAIL" "$SYSTEM_ADMIN_PASSWORD")

echo "▶ 2/5 테넌트 + 본사 관리자 등록 ($TENANT_NAME / $HQ_EMAIL)"
TENANT_RES=$(call POST /api/v1/system/tenants \
  "$(jq -nc --arg n "$TENANT_NAME" --arg e "$HQ_EMAIL" '{name:$n,cutoffTime:"12:00",admin:{email:$e,name:"본사관리자"}}')" \
  "$SYSTEM_TOKEN")
HQ_TEMP_PASSWORD=$(jq -r '.data.admin.temporaryPassword' <<<"$TENANT_RES")
TENANT_ID=$(jq -r '.data.tenant.id' <<<"$TENANT_RES")

# 본사 관리자는 임시 비밀번호 상태(TEMPORARY)로 생성된다 → 비밀번호 확정 전까지
# 일반 API 호출이 막히므로 여기서 바로 확정한다 (스펙 2.3)
echo "▶ 3/5 본사 관리자 비밀번호 확정"
HQ_TOKEN=$(login "$HQ_EMAIL" "$HQ_TEMP_PASSWORD")
HQ_TOKEN=$(call PUT /api/v1/users/me/password \
  "$(jq -nc --arg c "$HQ_TEMP_PASSWORD" --arg n "$SEED_PASSWORD" '{currentPassword:$c,newPassword:$n}')" \
  "$HQ_TOKEN" | jq -r '.data.accessToken')

echo "▶ 4/5 가맹점 등록 ($STORE_NAME)"
STORE_ID=$(call POST /api/v1/stores \
  "$(jq -nc --arg n "$STORE_NAME" '{name:$n,address:"서울시 강남구 테헤란로 1"}')" \
  "$HQ_TOKEN" | jq -r '.data.id')

echo "▶ 5/5 점주 계정 2개 등록 (확정 / 임시)"
# ① 비밀번호 확정 계정 — 앱의 정상 로그인·카탈로그 진입 시연용
OWNER_TEMP_PW=$(call POST /api/v1/users \
  "$(jq -nc --argjson s "$STORE_ID" --arg e "$OWNER_EMAIL" '{storeId:$s,email:$e,name:"강남점주"}')" \
  "$HQ_TOKEN" | jq -r '.data.temporaryPassword')
OWNER_TOKEN=$(login "$OWNER_EMAIL" "$OWNER_TEMP_PW")
call PUT /api/v1/users/me/password \
  "$(jq -nc --arg c "$OWNER_TEMP_PW" --arg n "$SEED_PASSWORD" '{currentPassword:$c,newPassword:$n}')" \
  "$OWNER_TOKEN" >/dev/null

# ② 임시 비밀번호 상태 유지 계정 — 앱의 PASSWORD_SETUP_REQUIRED 강제 이동 시연용
OWNER2_TEMP_PW=$(call POST /api/v1/users \
  "$(jq -nc --argjson s "$STORE_ID" --arg e "$OWNER_TEMP_EMAIL" '{storeId:$s,email:$e,name:"임시비번점주"}')" \
  "$HQ_TOKEN" | jq -r '.data.temporaryPassword')

# 카탈로그 — 목록이 비어 있으면 앱/웹 시연이 빈 화면이라 최소 표본을 넣는다.
# 한정 품목 1건 포함 (US-ORD-06 재고 차감 시연 대비)
echo "▶ 카탈로그 표본 품목 등록"
add_product() { # code name barcode category unit price
  call POST /api/v1/products \
    "$(jq -nc --arg c "$1" --arg n "$2" --arg b "$3" --arg g "$4" --arg u "$5" --argjson p "$6" \
       '{productCode:$c,name:$n,barcode:$b,category:$g,orderUnit:$u,unitPrice:$p}')" \
    "$HQ_TOKEN" | jq -r '.data.id'
}
add_product P001 "본죽 쌀 20kg"    8801111000011 "원재료" BOX 45000  >/dev/null
add_product P002 "일회용 용기 500개" 8801111000028 "부자재" BOX 32000  >/dev/null
LIMITED_ID=$(add_product P003 "한정 김치 5kg" 8801111000035 "원재료" EA 18000)
# 한정 품목 지정 + 본사 가용 재고 설정 (스펙 3.2.6 / 3.2.8)
call POST "/api/v1/products/$LIMITED_ID/limited" '{"availableQty":50}' "$HQ_TOKEN" >/dev/null

cat <<EOF

✓ 시드 완료 (tenantId=$TENANT_ID, storeId=$STORE_ID)

  역할          이메일                        비밀번호                상태
  ------------  ----------------------------  ----------------------  ------------------
  HQ_ADMIN      $HQ_EMAIL       (SEED_PASSWORD)         확정
  STORE_OWNER   $OWNER_EMAIL        (SEED_PASSWORD)         확정 — 정상 로그인 시연
  STORE_OWNER   $OWNER_TEMP_EMAIL   $OWNER2_TEMP_PW              임시 — passwordSetupRequired=true 시연

  카탈로그: 3건 (P001 쌀 / P002 용기 / P003 한정 김치 — 한정 품목, 가용 재고 50)

  ※ 임시 비밀번호는 등록 응답에서 1회만 노출된다 (스펙 2.4.9). 분실 시
     POST /api/v1/users/{userId}/temporary-password 로 재발급한다.
EOF
