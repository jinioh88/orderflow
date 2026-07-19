package com.orderflow.domain.iam;

import com.orderflow.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

/**
 * 테넌트(프랜차이즈 본사) — 데이터 격리의 단위 (04 §2.1).
 * 유일하게 tenant_id를 갖지 않는 애그리거트.
 */
@Entity
@Table(name = "tenant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tenant extends BaseEntity {

    public static final LocalTime DEFAULT_CUTOFF_TIME = LocalTime.of(12, 0);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantStatus status;

    @Column(name = "cutoff_time", nullable = false)
    private LocalTime cutoffTime;

    private Tenant(String name, LocalTime cutoffTime) {
        Assert.hasText(name, "테넌트 이름은 필수다");
        this.name = name;
        this.status = TenantStatus.ACTIVE;
        this.cutoffTime = cutoffTime != null ? cutoffTime : DEFAULT_CUTOFF_TIME;
    }

    /** 테넌트 등록 (US-AUTH-01). 마감 시각 미지정 시 12:00 (api-spec 2.4.1). */
    public static Tenant register(String name, LocalTime cutoffTime) {
        return new Tenant(name, cutoffTime);
    }

    public boolean isActive() {
        return status == TenantStatus.ACTIVE;
    }
}
