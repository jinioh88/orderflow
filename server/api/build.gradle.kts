// API 모듈 — REST 서버 실행 모듈. 응용 서비스·컨트롤러.
plugins {
    id("org.springframework.boot")
}

dependencies {
    "implementation"(project(":infra"))
    "implementation"("org.springframework.boot:spring-boot-starter-webmvc")
    "implementation"("org.springframework.boot:spring-boot-starter-validation")
    "testImplementation"("org.springframework.boot:spring-boot-starter-webmvc-test")
    "testImplementation"("org.springframework.boot:spring-boot-starter-data-jpa-test")
}
