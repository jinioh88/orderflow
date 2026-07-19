// 인프라 모듈 — 리포지토리 구현(QueryDSL)·영속성 설정·외부 시스템 연동.
val querydslVersion = "7.0"

dependencies {
    "api"(project(":domain"))
    "implementation"("org.springframework.boot:spring-boot-starter-data-jpa")
    "implementation"("org.springframework.boot:spring-boot-starter-data-redis")
    "implementation"("io.github.openfeign.querydsl:querydsl-jpa:$querydslVersion")
    "runtimeOnly"("com.mysql:mysql-connector-j")
}
