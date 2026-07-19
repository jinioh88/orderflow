package com.orderflow.api.iam.dto;

import com.orderflow.api.common.response.KstTimes;
import com.orderflow.domain.iam.Store;
import com.orderflow.domain.iam.StoreStatus;
import com.orderflow.domain.iam.User;
import com.orderflow.domain.iam.UserRole;
import com.orderflow.domain.iam.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/**
 * 가맹점·계정 관리 요청/응답 (api-spec 2.4.6~2.4.12)
 */
public final class IamDtos {

    private IamDtos() {
    }

    public record StoreRegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 255) String address) {
    }

    public record StoreResponse(Long id, String name, StoreStatus status, String address, OffsetDateTime createdAt) {

        public static StoreResponse from(Store store) {
            return new StoreResponse(store.getId(), store.getName(), store.getStatus(),
                    store.getAddress(), KstTimes.toOffset(store.getCreatedAt()));
        }
    }

    public record UserRegisterRequest(
            @NotNull Long storeId,
            @NotBlank @Email @Size(max = 100) String email,
            @NotBlank @Size(max = 50) String name) {
    }

    public record UserResponse(Long id, String email, String name, UserRole role, Long storeId,
                               UserStatus status, boolean passwordSetupRequired, OffsetDateTime createdAt) {

        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getRole(),
                    user.getStoreId(), user.getStatus(), user.requiresPasswordSetup(),
                    KstTimes.toOffset(user.getCreatedAt()));
        }
    }

    /** 등록 응답 — 임시 비밀번호는 이 응답에서 1회만 노출된다 (api-spec 2.3, 2.4.9) */
    public record UserCreatedResponse(Long id, String email, String name, UserRole role, Long storeId,
                                      UserStatus status, boolean passwordSetupRequired, String temporaryPassword) {

        public static UserCreatedResponse of(User user, String temporaryPassword) {
            return new UserCreatedResponse(user.getId(), user.getEmail(), user.getName(), user.getRole(),
                    user.getStoreId(), user.getStatus(), user.requiresPasswordSetup(), temporaryPassword);
        }
    }

    public record TemporaryPasswordResponse(String temporaryPassword) {
    }
}
