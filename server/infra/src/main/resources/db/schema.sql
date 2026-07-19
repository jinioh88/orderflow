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
