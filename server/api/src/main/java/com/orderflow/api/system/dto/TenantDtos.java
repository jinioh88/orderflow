package com.orderflow.api.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.orderflow.domain.iam.Tenant;
import com.orderflow.domain.iam.TenantStatus;
import com.orderflow.domain.iam.User;
import com.orderflow.domain.iam.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

/**
 * 테넌트 등록 요청/응답 (api-spec 2.4.1)
 */
public final class TenantDtos {

    private TenantDtos() {
    }

    public record TenantRegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @JsonFormat(pattern = "HH:mm") LocalTime cutoffTime,
            @NotNull @Valid AdminPart admin) {

        public record AdminPart(
                @NotBlank @Email @Size(max = 100) String email,
                @NotBlank @Size(max = 50) String name) {
        }
    }

    public record TenantRegisterResponse(TenantPart tenant, AdminPart admin) {

        public record TenantPart(
                Long id,
                String name,
                TenantStatus status,
                @JsonFormat(pattern = "HH:mm") LocalTime cutoffTime) {
        }

        public record AdminPart(Long id, String email, String name, UserRole role, String temporaryPassword) {
        }

        public static TenantRegisterResponse of(Tenant tenant, User admin, String temporaryPassword) {
            return new TenantRegisterResponse(
                    new TenantPart(tenant.getId(), tenant.getName(), tenant.getStatus(), tenant.getCutoffTime()),
                    new AdminPart(admin.getId(), admin.getEmail(), admin.getName(), admin.getRole(), temporaryPassword));
        }
    }
}
