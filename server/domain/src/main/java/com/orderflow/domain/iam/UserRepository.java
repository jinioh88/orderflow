package com.orderflow.domain.iam;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends Repository<User, Long> {

    User save(User user);

    /** 단건 조회 JPQL 강제 — em.find는 테넌트 필터를 우회한다 (설계 노트 §5-2). */
    @Query("select u from User u where u.id = :id")
    Optional<User> findById(@Param("id") Long id);

    /**
     * 로그인 조회 — 이메일은 전역 유일. 인증 전이라 테넌트 컨텍스트가 없으므로
     * 호출자는 {@code TenantContext.runUnfiltered(...)}로 감싼다 (화이트리스트 대상).
     */
    Optional<User> findByEmail(String email);

    /** 이메일 전역 중복 검사 (api-spec `EMAIL_DUPLICATED`) — 전역 검사이므로 UNFILTERED 컨텍스트에서 호출한다. */
    boolean existsByEmail(String email);

    /** 가맹점 비활성화 시 소속 사용자 토큰 무효화 대상 조회 (api-spec 2.2 무효화 이벤트) */
    List<User> findAllByStoreId(Long storeId);

    /** 계정 목록 (api-spec 2.4.11) — 선택 필터 3종. 테넌트 스코프는 필터가 강제한다. */
    @Query("""
            select u from User u
            where (:storeId is null or u.storeId = :storeId)
              and (:status is null or u.status = :status)
              and (:role is null or u.role = :role)
            """)
    Page<User> findAllByFilters(@Param("storeId") Long storeId,
                                @Param("status") UserStatus status,
                                @Param("role") UserRole role,
                                Pageable pageable);
}
