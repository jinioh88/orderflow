package com.orderflow.api.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orderflow.api.support.ApiIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * 한정 품목 지정·해제·가용 재고 설정 API 통합 테스트 (US-CAT-04, api-spec 3.2.6~3.2.8)
 * 재고 차감·복원은 M2(ORD) 범위 — 여기서는 지정과 수량 설정까지만 다룬다.
 */
class LimitedProductApiTest extends ApiIntegrationTest {

    private TenantSetup bonjuk;
    private long productId;

    @BeforeEach
    void setUpFixture() throws Exception {
        bonjuk = createTenantSetup("bonjuk");
        String response = mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productCode", "P-0001", "name", "한정 만두", "barcode", "8801111111111",
                                "category", "냉동식품", "orderUnit", "BOX", "unitPrice", 12000))))
                .andReturn().getResponse().getContentAsString();
        productId = objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private String qtyBody(int qty) throws Exception {
        return objectMapper.writeValueAsString(Map.of("availableQty", qty));
    }

    @Test
    @DisplayName("한정 품목 지정 — 가용 재고와 함께 설정되고 단건 조회에 반영된다 (3.2.6)")
    void designateLimited() throws Exception {
        mockMvc.perform(post("/api/v1/products/" + productId + "/limited")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(qtyBody(100)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.limited").value(true))
                .andExpect(jsonPath("$.data.availableQty").value(100));

        mockMvc.perform(get("/api/v1/products/" + productId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.limited").value(true))
                .andExpect(jsonPath("$.data.availableQty").value(100));
    }

    @Test
    @DisplayName("재지정은 409 ALREADY_LIMITED, 음수 수량은 400 (3.2.6)")
    void designateGuards() throws Exception {
        mockMvc.perform(post("/api/v1/products/" + productId + "/limited")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(qtyBody(100)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/products/" + productId + "/limited")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(qtyBody(50)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ALREADY_LIMITED"));

        mockMvc.perform(put("/api/v1/products/" + productId + "/hq-stock")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(qtyBody(-1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("해제 — 가용 재고가 사라지고, 재해제는 409 NOT_LIMITED (3.2.7)")
    void releaseLimited() throws Exception {
        mockMvc.perform(post("/api/v1/products/" + productId + "/limited")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(qtyBody(100)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/products/" + productId + "/limited")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.limited").value(false))
                .andExpect(jsonPath("$.data.availableQty").doesNotExist());

        mockMvc.perform(delete("/api/v1/products/" + productId + "/limited")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("NOT_LIMITED"));
    }

    @Test
    @DisplayName("가용 재고 설정은 절대값 교체 — 한정 아니면 409 NOT_LIMITED (3.2.8)")
    void changeAvailableQty() throws Exception {
        // 한정 지정 전 설정 시도
        mockMvc.perform(put("/api/v1/products/" + productId + "/hq-stock")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(qtyBody(10)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("NOT_LIMITED"));

        mockMvc.perform(post("/api/v1/products/" + productId + "/limited")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(qtyBody(100)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/products/" + productId + "/hq-stock")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(qtyBody(250)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productId").value(productId))
                .andExpect(jsonPath("$.data.availableQty").value(250));
    }

    @Test
    @DisplayName("타 테넌트 상품의 한정 지정은 404 (NFR-2.4)")
    void crossTenantDesignateIs404() throws Exception {
        TenantSetup kimbap = createTenantSetup("kimbap");

        mockMvc.perform(post("/api/v1/products/" + productId + "/limited")
                        .header(HttpHeaders.AUTHORIZATION, bearer(kimbap.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(qtyBody(100)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }
}
