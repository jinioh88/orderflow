package com.orderflow.domain.iam;

import com.orderflow.domain.common.BaseEntity;
import com.orderflow.domain.common.InvalidStateTransitionException;
import com.orderflow.domain.common.TenantFilter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Filter;
import org.springframework.util.Assert;

/**
 * 가맹점 — 발주 주체 (04 §2.1).
 * 불변식: 비활성 가맹점의 사용자로는 로그인 불가 (로그인 가드는 인증 유스케이스에서 이 상태를 본다).
 */
@Entity
@Table(name = "store", indexes = @Index(name = "idx_store_tenant_status", columnList = "tenant_id, status"))
@Filter(name = TenantFilter.NAME, condition = TenantFilter.CONDITION)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoreStatus status;

    /** 배송지 */
    @Column(length = 255)
    private String address;

    private Store(Long tenantId, String name, String address) {
        Assert.notNull(tenantId, "가맹점은 테넌트 소속이어야 한다");
        Assert.hasText(name, "가맹점 이름은 필수다");
        this.tenantId = tenantId;
        this.name = name;
        this.status = StoreStatus.ACTIVE;
        this.address = address;
    }

    /** 가맹점 등록 (US-AUTH-02, api-spec 2.4.6). */
    public static Store register(Long tenantId, String name, String address) {
        return new Store(tenantId, name, address);
    }

    /** 가맹점 비활성화 (US-AUTH-02, api-spec 2.4.8) — 소속 사용자 로그인 차단의 근거 상태. */
    public void deactivate() {
        if (status == StoreStatus.INACTIVE) {
            throw new InvalidStateTransitionException("이미 비활성화된 가맹점입니다.");
        }
        this.status = StoreStatus.INACTIVE;
    }

    public boolean isActive() {
        return status == StoreStatus.ACTIVE;
    }
}
