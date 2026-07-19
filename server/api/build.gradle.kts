// API 모듈 — REST 서버 실행 모듈. 응용 서비스·컨트롤러.
plugins {
    id("org.springframework.boot")
}

// JWT: jjwt — 표준 클레임 API·서명 검증이 안정적이고 Boot 의존성과 충돌 없음 (0.12.x)
val jjwtVersion = "0.12.6"

dependencies {
    "implementation"(project(":infra"))
    "implementation"("org.springframework.boot:spring-boot-starter-webmvc")
    "implementation"("org.springframework.boot:spring-boot-starter-validation")
    "implementation"("org.springframework.boot:spring-boot-starter-security")
    "implementation"("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    "runtimeOnly"("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    "runtimeOnly"("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")
    "testImplementation"("org.springframework.boot:spring-boot-starter-webmvc-test")
    "testImplementation"("org.springframework.boot:spring-boot-starter-data-jpa-test")
    "testImplementation"("org.springframework.boot:spring-boot-starter-data-redis") // 테스트 픽스처 정리(flushDb)용
}
