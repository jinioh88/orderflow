package com.orderflow.domain.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderflow.domain.common.InvalidStateTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final String ENCODED = "$2a$10$encoded";

    @Nested
    @DisplayName("생성 불변식 — 역할별 소속 규칙")
    class CreationInvariants {

        @Test
        @DisplayName("STORE_OWNER는 storeId 없이 만들 수 없다")
        void storeOwnerRequiresStore() {
            assertThatIllegalArgumentException().isThrownBy(() ->
                    User.registerStoreOwner(1L, null, "owner@test.com", ENCODED, "박점주"));
        }

        @Test
        @DisplayName("HQ_ADMIN은 tenantId가 필수다")
        void hqAdminRequiresTenant() {
            assertThatIllegalArgumentException().isThrownBy(() ->
                    User.registerHqAdmin(null, "admin@test.com", ENCODED, "김운영"));
        }

        @Test
        @DisplayName("SYSTEM은 테넌트·가맹점에 소속되지 않는다")
        void systemHasNoTenant() {
            User system = User.registerSystem("system@orderflow.io", ENCODED, "시스템 관리자");

            assertThat(system.getTenantId()).isNull();
            assertThat(system.getStoreId()).isNull();
            assertThat(system.getRole()).isEqualTo(UserRole.SYSTEM);
        }
    }

    @Nested
    @DisplayName("임시 비밀번호 상태 (US-AUTH-02·03)")
    class TemporaryPassword {

        @Test
        @DisplayName("관리자가 등록한 계정은 임시 상태로 시작한다")
        void registeredAccountStartsTemporary() {
            User owner = User.registerStoreOwner(1L, 7L, "owner@test.com", ENCODED, "박점주");

            assertThat(owner.requiresPasswordSetup()).isTrue();
        }

        @Test
        @DisplayName("SYSTEM 부트스트랩 계정은 임시 상태가 아니다")
        void systemAccountIsConfirmed() {
            User system = User.registerSystem("system@orderflow.io", ENCODED, "시스템 관리자");

            assertThat(system.requiresPasswordSetup()).isFalse();
        }

        @Test
        @DisplayName("비밀번호 설정으로 임시 상태가 해제된다")
        void confirmPasswordClearsTemporaryState() {
            User owner = User.registerStoreOwner(1L, 7L, "owner@test.com", ENCODED, "박점주");

            owner.confirmPassword("$2a$10$newEncoded");

            assertThat(owner.requiresPasswordSetup()).isFalse();
            assertThat(owner.getPassword()).isEqualTo("$2a$10$newEncoded");
        }

        @Test
        @DisplayName("임시 비밀번호 재발급은 임시 상태로 되돌린다")
        void reissueReturnsToTemporaryState() {
            User owner = User.registerStoreOwner(1L, 7L, "owner@test.com", ENCODED, "박점주");
            owner.confirmPassword("$2a$10$newEncoded");

            owner.issueTemporaryPassword("$2a$10$reissued");

            assertThat(owner.requiresPasswordSetup()).isTrue();
            assertThat(owner.getPassword()).isEqualTo("$2a$10$reissued");
        }
    }

    @Nested
    @DisplayName("비활성화 (US-AUTH-02)")
    class Deactivation {

        @Test
        @DisplayName("활성 계정은 비활성화할 수 있다")
        void deactivateActiveAccount() {
            User owner = User.registerStoreOwner(1L, 7L, "owner@test.com", ENCODED, "박점주");

            owner.deactivate();

            assertThat(owner.isActive()).isFalse();
        }

        @Test
        @DisplayName("이미 비활성인 계정을 다시 비활성화하면 도메인 예외가 난다")
        void deactivateTwiceThrows() {
            User owner = User.registerStoreOwner(1L, 7L, "owner@test.com", ENCODED, "박점주");
            owner.deactivate();

            assertThatThrownBy(owner::deactivate)
                    .isInstanceOf(InvalidStateTransitionException.class);
        }
    }
}
