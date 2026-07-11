// 배치 모듈 — 마감 집계 실행 모듈 (M2에서 Spring Batch 의존성 추가).
plugins {
    id("org.springframework.boot")
}

dependencies {
    "implementation"(project(":infra"))
    "implementation"("org.springframework.boot:spring-boot-starter")
}
