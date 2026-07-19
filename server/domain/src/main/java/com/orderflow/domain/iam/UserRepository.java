package com.orderflow.domain.iam;

import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface UserRepository extends Repository<User, Long> {

    User save(User user);

    Optional<User> findById(Long id);

    /** 로그인 조회 — 이메일은 전역 유일이므로 테넌트 스코프 밖에서 찾는다 (인증 전이라 테넌트 컨텍스트가 없다). */
    Optional<User> findByEmail(String email);

    /** 이메일 전역 중복 검사 (api-spec `EMAIL_DUPLICATED`) */
    boolean existsByEmail(String email);
}
