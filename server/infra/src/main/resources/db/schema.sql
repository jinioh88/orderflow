-- ============================================================
-- OrderFlow 스키마 — 수동 관리 DDL (Flyway 미도입 결정, PM 2026-07-17)
-- 기준 문서: server/docs/erd.md — 스키마 변경 시 erd.md와 이 파일을 함께 갱신한다.
--
-- * 테스트 프로필: 매 실행 시 이 스크립트를 자동 적용(drop & create)하고
--   Hibernate ddl-auto=validate로 엔티티 매핑과의 드리프트를 검증한다.
-- * 로컬 적용(주의 — 기존 데이터 삭제):
--   docker exec -i orderflow-mysql mysql -uorderflow -porderflow-local orderflow \
--     < infra/src/main/resources/db/schema.sql
-- ============================================================

-- FK 역순으로 drop
DROP TABLE IF EXISTS hq_stock;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS store;
DROP TABLE IF EXISTS tenant;

-- 3.1 Identity & Access ---------------------------------------

CREATE TABLE tenant (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    cutoff_time TIME         NOT NULL DEFAULT '12:00',
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE store (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id  BIGINT       NOT NULL,
    name       VARCHAR(100) NOT NULL,
    status     VARCHAR(20)  NOT NULL,
    address    VARCHAR(255) NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_store_tenant_status (tenant_id, status),
    CONSTRAINT fk_store_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE users (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id       BIGINT       NULL COMMENT 'SYSTEM 계정만 NULL (AUTH-1 승인 2026-07-19)',
    store_id        BIGINT       NULL COMMENT '본사 소속이면 NULL',
    email           VARCHAR(100) NOT NULL COMMENT '로그인 ID — 전역 유일',
    password        VARCHAR(100) NOT NULL COMMENT 'bcrypt',
    name            VARCHAR(50)  NOT NULL,
    role            VARCHAR(20)  NOT NULL COMMENT 'SYSTEM/HQ_ADMIN/HQ_MANAGER/STORE_OWNER/STORE_STAFF',
    password_status VARCHAR(20)  NOT NULL COMMENT 'TEMPORARY/CONFIRMED',
    status          VARCHAR(20)  NOT NULL COMMENT 'ACTIVE/INACTIVE',
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
    CONSTRAINT fk_users_store FOREIGN KEY (store_id) REFERENCES store (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 3.2 Catalog -------------------------------------------------

CREATE TABLE product (
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    tenant_id    BIGINT         NOT NULL,
    product_code VARCHAR(30)    NOT NULL COMMENT '품목코드 — 등록 후 수정 불가, 엑셀 매칭 키',
    name         VARCHAR(100)   NOT NULL,
    barcode      VARCHAR(30)    NOT NULL,
    category     VARCHAR(50)    NOT NULL COMMENT 'MVP는 단순 문자열 (계층 카테고리 미도입)',
    order_unit   VARCHAR(20)    NOT NULL COMMENT '발주 단위 (BOX, EA, KG …)',
    unit_price   DECIMAL(15, 2) NOT NULL COMMENT '현재 단가 — 주문 시 라인에 스냅샷',
    is_limited   BOOLEAN        NOT NULL DEFAULT FALSE COMMENT '한정 품목 여부',
    status       VARCHAR(20)    NOT NULL COMMENT 'ON_SALE/SUSPENDED',
    created_at   DATETIME(6)    NOT NULL,
    updated_at   DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_tenant_code (tenant_id, product_code),
    UNIQUE KEY uk_product_tenant_barcode (tenant_id, barcode) COMMENT '바코드 스캔 조회 인덱스 겸용 (US-GRN-01)',
    CONSTRAINT fk_product_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE hq_stock (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    tenant_id     BIGINT      NOT NULL,
    product_id    BIGINT      NOT NULL,
    available_qty INT         NOT NULL COMMENT 'DB 레벨 최후 방어선 — Redlock이 뚫려도 음수 불가 (NFR-3.1)',
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_hq_stock_product (product_id),
    CONSTRAINT fk_hq_stock_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT chk_hq_stock_available_qty CHECK (available_qty >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
