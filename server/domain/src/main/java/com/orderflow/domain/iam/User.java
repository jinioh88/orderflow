package com.orderflow.domain.iam;

import com.orderflow.domain.common.BaseEntity;
import com.orderflow.domain.common.InvalidStateTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

/**
 * 사용자 계정 (04 §2.1) — 테이블명 users는 MySQL 예약어 회피.
 * 불변식: STORE_* 역할은 store_id 필수, HQ_* 역할은 store_id 없음, SYSTEM만 tenant_id 없음.
 * 비밀번호는 항상 bcrypt 해시로 전달받아 저장한다 (NFR-2.5) — 평문은 도메인에 들어오지 않는다.
 */
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** SYSTEM 계정만 null (AUTH-1 승인, api-spec 2.1) */
    @Column(name = "tenant_id")
    private Long tenantId;

    /** 본사 소속이면 null */
    @Column(name = "store_id")
    private Long storeId;

    /** 로그인 ID — 전역 유일 */
    @Column(nullable = false, length = 100)
    private String email;

    /** bcrypt 해시 */
    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    /** 임시 비밀번호 상태 (US-AUTH-02·03) — TEMPORARY면 비밀번호 설정 외 API 차단 */
    @Enumerated(EnumType.STRING)
    @Column(name = "password_status", nullable = false, length = 20)
    private PasswordStatus passwordStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    private User(Long tenantId, Long storeId, String email, String encodedPassword,
                 String name, UserRole role, PasswordStatus passwordStatus) {
        Assert.hasText(email, "이메일은 필수다");
        Assert.hasText(encodedPassword, "비밀번호는 필수다");
        Assert.hasText(name, "이름은 필수다");
        Assert.notNull(role, "역할은 필수다");
        if (role == UserRole.SYSTEM) {
            Assert.isNull(tenantId, "SYSTEM 계정은 테넌트에 소속되지 않는다");
        } else {
            Assert.notNull(tenantId, role + " 계정은 테넌트 소속이어야 한다");
        }
        if (role.isStoreRole()) {
            Assert.notNull(storeId, role + " 계정은 소속 가맹점이 있어야 한다");
        } else {
            Assert.isNull(storeId, role + " 계정은 가맹점에 소속될 수 없다");
        }
        this.tenantId = tenantId;
        this.storeId = storeId;
        this.email = email;
        this.password = encodedPassword;
        this.name = name;
        this.role = role;
        this.passwordStatus = passwordStatus;
        this.status = UserStatus.ACTIVE;
    }

    /** 최초 SYSTEM 계정 — 환경변수 부트스트랩 전용 (api-spec 2.1). 비밀번호는 본인이 정한 값이므로 임시 상태가 아니다. */
    public static User registerSystem(String email, String encodedPassword, String name) {
        return new User(null, null, email, encodedPassword, name, UserRole.SYSTEM, PasswordStatus.CONFIRMED);
    }

    /** 최초 본사 관리자 — 테넌트 등록 시 임시 비밀번호로 생성 (US-AUTH-01, api-spec 2.4.1). */
    public static User registerHqAdmin(Long tenantId, String email, String encodedTemporaryPassword, String name) {
        return new User(tenantId, null, email, encodedTemporaryPassword, name, UserRole.HQ_ADMIN, PasswordStatus.TEMPORARY);
    }

    /** 점주 계정 — 임시 비밀번호로 생성 (US-AUTH-02, api-spec 2.4.9). */
    public static User registerStoreOwner(Long tenantId, Long storeId, String email,
                                          String encodedTemporaryPassword, String name) {
        return new User(tenantId, storeId, email, encodedTemporaryPassword, name,
                UserRole.STORE_OWNER, PasswordStatus.TEMPORARY);
    }

    /** 본인 비밀번호 설정 — 임시 상태 해제 (api-spec 2.4.4). 일반 변경에도 동일하게 쓴다. */
    public void confirmPassword(String encodedNewPassword) {
        Assert.hasText(encodedNewPassword, "비밀번호는 필수다");
        this.password = encodedNewPassword;
        this.passwordStatus = PasswordStatus.CONFIRMED;
    }

    /** 임시 비밀번호 재발급 — 임시 상태로 되돌린다 (api-spec 2.4.10). */
    public void issueTemporaryPassword(String encodedTemporaryPassword) {
        Assert.hasText(encodedTemporaryPassword, "비밀번호는 필수다");
        this.password = encodedTemporaryPassword;
        this.passwordStatus = PasswordStatus.TEMPORARY;
    }

    /** 계정 비활성화 (US-AUTH-02, api-spec 2.4.12) — 로그인·재발급 차단의 근거 상태. */
    public void deactivate() {
        if (status == UserStatus.INACTIVE) {
            throw new InvalidStateTransitionException("이미 비활성화된 계정입니다.");
        }
        this.status = UserStatus.INACTIVE;
    }

    public boolean requiresPasswordSetup() {
        return passwordStatus == PasswordStatus.TEMPORARY;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}
