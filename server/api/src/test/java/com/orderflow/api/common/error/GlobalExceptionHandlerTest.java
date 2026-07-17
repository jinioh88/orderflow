package com.orderflow.api.common.error;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 공통 규약 계약 테스트 — api-spec.md 1.3(래퍼)·1.4(에러 코드)가 실제 응답과 일치하는지 검증한다.
 * 웹·앱 인터셉터가 이 형식에 의존하므로 이 테스트가 깨지면 계약 위반이다.
 */
@WebMvcTest(ErrorContractTestController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 성공_응답은_data_래퍼로_감싼다() throws Exception {
        mockMvc.perform(get("/test/success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("김치만두"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void 비즈니스_예외는_에러코드의_상태와_코드로_응답한다() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.message").value("이미 마감된 회차입니다."))
                .andExpect(jsonPath("$.error.details").doesNotExist());
    }

    @Test
    void 없는_리소스는_404_RESOURCE_NOT_FOUND() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void 매핑되지_않은_경로도_같은_에러_형식의_404() throws Exception {
        mockMvc.perform(get("/no-such-path"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void 필드_검증_실패는_400_VALIDATION_ERROR와_details를_담는다() throws Exception {
        mockMvc.perform(post("/test/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"quantity\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.length()").value(2))
                .andExpect(jsonPath("$.error.details[?(@.field == 'name')].reason").value("필수 입력입니다."))
                .andExpect(jsonPath("$.error.details[?(@.field == 'quantity')].reason").value("1 이상이어야 합니다."));
    }

    @Test
    void 본문_파싱_불가는_400_INVALID_REQUEST() throws Exception {
        mockMvc.perform(post("/test/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void 파라미터_타입_불일치는_400_INVALID_REQUEST() throws Exception {
        mockMvc.perform(get("/test/typed").param("value", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void 파라미터_누락은_400_INVALID_REQUEST() throws Exception {
        mockMvc.perform(get("/test/typed"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void 허용되지_않은_메서드는_405_METHOD_NOT_ALLOWED() throws Exception {
        mockMvc.perform(post("/test/success"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void 처리되지_않은_예외는_500_INTERNAL_ERROR로_원인을_숨긴다() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("일시적인 오류가 발생했습니다."));
    }
}
