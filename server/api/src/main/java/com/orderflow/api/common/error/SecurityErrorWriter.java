package com.orderflow.api.common.error;

import com.orderflow.api.common.response.ErrorResponse;
import com.orderflow.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Security 필터 계층의 에러 응답 — @RestControllerAdvice가 닿지 않는 지점에서도
 * 공통 에러 포맷(api-spec 1.3)을 유지한다 (스펙 1.4의 401/403 계약).
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.status());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(errorCode));
    }
}
