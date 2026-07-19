package com.orderflow.api.auth;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * 임시 비밀번호 생성 — 랜덤 12자, 영문 대소문자+숫자 (api-spec 2.3).
 * 사람이 옮겨 적는 값이라 혼동 문자(0/O, 1/l/I)는 제외한다.
 */
@Component
public class TemporaryPasswordGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int LENGTH = 12;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
