// 도메인 모듈 — 애그리거트(@Entity)·리포지토리 인터페이스·도메인 이벤트.
// QueryDSL은 원본(com.querydsl) 관리 중단으로 Hibernate 7 호환 OpenFeign 포크를 사용한다.
val querydslVersion = "7.0"

dependencies {
    "api"(project(":common"))
    "implementation"("org.springframework.boot:spring-boot-starter-data-jpa")
    "implementation"("io.github.openfeign.querydsl:querydsl-core:$querydslVersion")
    "annotationProcessor"("io.github.openfeign.querydsl:querydsl-apt:$querydslVersion:jpa")
    "annotationProcessor"("jakarta.persistence:jakarta.persistence-api")
    "testImplementation"("org.springframework.boot:spring-boot-starter-test")
}
