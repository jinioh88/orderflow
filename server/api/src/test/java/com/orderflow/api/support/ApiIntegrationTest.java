package com.orderflow.api.support;

import com.orderflow.api.auth.jwt.JwtTokenProvider;
import com.orderflow.domain.common.TenantContext;
import com.orderflow.domain.iam.Store;
import com.orderflow.domain.iam.StoreRepository;
import com.orderflow.domain.iam.Tenant;
import com.orderflow.domain.iam.TenantRepository;
import com.orderflow.domain.iam.User;
import com.orderflow.domain.iam.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * API(HTTP) 통합 테스트 베이스 — 실제 Security 필터 체인 + JWT + MySQL + Redis.
 *
 * @Transactional 테스트 트랜잭션을 쓰지 않는다 — 프로덕션과 동일하게 "인증 필터의 컨텍스트 설정 →
 * 서비스 트랜잭션 시작(테넌트 필터 활성화)" 순서를 재현해야 하기 때문. 픽스처는 커밋되고
 * 테스트 종료 시 전부 삭제한다 (테스트 전용 DB + Redis DB 1).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiIntegrationTest {

    protected static final String PASSWORD = "Password1";
    private static String cachedEncodedPassword;

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected TransactionTemplate transactionTemplate;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    protected TenantRepository tenantRepository;
    @Autowired
    protected StoreRepository storeRepository;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;
    @Autowired
    protected JwtTokenProvider tokenProvider;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @AfterEach
    void cleanUpFixtures() {
        TenantContext.clear();
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("DELETE FROM store");
        jdbcTemplate.update("DELETE FROM tenant");
        // 리프레시 토큰 정리 — 테스트 전용 Redis DB(1)만 비운다
        redisTemplate.execute(connection -> {
            connection.serverCommands().flushDb();
            return null;
        }, true);
    }

    /** 테넌트 + 본사 관리자 + 가맹점 + 점주 픽스처 (비밀번호는 전부 {@link #PASSWORD}, 임시 상태 해제됨) */
    protected TenantSetup createTenantSetup(String prefix) {
        return transactionTemplate.execute(status -> {
            Tenant tenant = tenantRepository.save(Tenant.register(prefix + " 본사", null));
            User admin = userRepository.save(confirmed(
                    User.registerHqAdmin(tenant.getId(), prefix + "-admin@test.com", encodedPassword(), "관리자")));
            Store store = storeRepository.save(Store.register(tenant.getId(), prefix + " 1호점", null));
            User owner = userRepository.save(confirmed(User.registerStoreOwner(
                    tenant.getId(), store.getId(), prefix + "-owner@test.com", encodedPassword(), "점주")));
            return new TenantSetup(tenant, admin, store, owner);
        });
    }

    protected User createSystemAccount() {
        return transactionTemplate.execute(status -> userRepository.save(
                User.registerSystem("system@test.com", encodedPassword(), "시스템 관리자")));
    }

    protected User saveUser(User user) {
        return transactionTemplate.execute(status -> userRepository.save(user));
    }

    /** 픽스처 변경용 — 조회가 필요한 변경은 fail-closed 필터에 걸리지 않게 UNFILTERED 트랜잭션에서 실행 */
    protected void inUnfilteredTx(Runnable action) {
        TenantContext.runUnfiltered(() -> {
            transactionTemplate.executeWithoutResult(status -> action.run());
            return null;
        });
    }

    protected String bearer(User user) {
        return "Bearer " + tokenProvider.createAccessToken(user);
    }

    /** bcrypt는 느리다 — 한 번 인코딩해 재사용 */
    protected String encodedPassword() {
        if (cachedEncodedPassword == null) {
            cachedEncodedPassword = passwordEncoder.encode(PASSWORD);
        }
        return cachedEncodedPassword;
    }

    private User confirmed(User user) {
        user.confirmPassword(encodedPassword());
        return user;
    }

    public record TenantSetup(Tenant tenant, User admin, Store store, User owner) {
    }
}
