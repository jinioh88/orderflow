package com.orderflow.api.auth;

import com.orderflow.domain.common.TenantContext;
import com.orderflow.domain.iam.User;
import com.orderflow.domain.iam.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * 최초 SYSTEM 계정 부트스트랩 (AUTH-1 승인, api-spec 2.1) —
 * 앱 기동 시 환경변수(SYSTEM_ADMIN_EMAIL/PASSWORD)로 멱등 생성. 시크릿을 스크립트·코드에 남기지 않는다 (NFR-2.5).
 */
@Slf4j
@Component
public class SystemAccountBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;
    private final String email;
    private final String password;

    public SystemAccountBootstrap(UserRepository userRepository,
                                  PasswordEncoder passwordEncoder,
                                  TransactionTemplate transactionTemplate,
                                  @Value("${orderflow.system-admin.email:}") String email,
                                  @Value("${orderflow.system-admin.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = transactionTemplate;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            log.info("SYSTEM 계정 부트스트랩 생략 — SYSTEM_ADMIN_EMAIL/PASSWORD 미설정");
            return;
        }
        TenantContext.runUnfiltered(() -> transactionTemplate.execute(status -> {
            if (userRepository.existsByEmail(email)) {
                log.info("SYSTEM 계정 이미 존재 — 부트스트랩 생략 ({})", email);
                return null;
            }
            userRepository.save(User.registerSystem(email, passwordEncoder.encode(password), "시스템 관리자"));
            log.info("SYSTEM 계정 생성 완료 ({})", email);
            return null;
        }));
    }
}
