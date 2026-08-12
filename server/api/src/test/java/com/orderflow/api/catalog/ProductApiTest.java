package com.orderflow.api.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orderflow.api.support.ApiIntegrationTest;
import com.orderflow.domain.iam.User;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * 상품 CRUD·판매중지 API 통합 테스트 (US-CAT-01, api-spec 3.2.1~3.2.5)
 * 교차 테넌트 404는 2-테넌트 픽스처로 재검증한다 (NFR-2.4).
 */
class ProductApiTest extends ApiIntegrationTest {

    private TenantSetup bonjuk;
    private TenantSetup kimbap;

    @BeforeEach
    void setUpTenants() {
        bonjuk = createTenantSetup("bonjuk");
        kimbap = createTenantSetup("kimbap");
    }

    private Map<String, Object> productBody(String code, String barcode, long unitPrice) {
        return Map.of("productCode", code, "name", "김치만두 1kg", "barcode", barcode,
                "category", "냉동식품", "orderUnit", "BOX", "unitPrice", unitPrice);
    }

    private long createProduct(User user, String code, String barcode) throws Exception {
        String response = mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productBody(code, barcode, 12000))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    @Test
    @DisplayName("상품 등록 — 판매중·한정 아님으로 시작하고 상품 객체를 반환한다 (3.2.1)")
    void registerProduct() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productBody("P-0001", "8801111111111", 12000))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.productCode").value("P-0001"))
                .andExpect(jsonPath("$.data.unitPrice").value(12000))
                .andExpect(jsonPath("$.data.limited").value(false))
                .andExpect(jsonPath("$.data.availableQty").doesNotExist())
                .andExpect(jsonPath("$.data.status").value("ON_SALE"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("품목코드·바코드 중복은 테넌트 스코프 409 — 타 테넌트와는 충돌하지 않는다 (3.4)")
    void duplicationIsTenantScoped() throws Exception {
        createProduct(bonjuk.admin(), "P-0001", "8801111111111");

        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productBody("P-0001", "8809999999999", 1000))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PRODUCT_CODE_DUPLICATED"));

        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productBody("P-0002", "8801111111111", 1000))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("BARCODE_DUPLICATED"));

        // 타 테넌트는 같은 품목코드·바코드로 등록 가능
        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(kimbap.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productBody("P-0001", "8801111111111", 1000))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("음수 단가는 400 VALIDATION_ERROR")
    void negativeUnitPriceRejected() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productBody("P-0001", "8801111111111", -1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("unitPrice"));
    }

    @Test
    @DisplayName("수정은 전체 교체 — 품목코드는 유지되고, 타 상품 바코드는 409 (3.2.4)")
    void updateProduct() throws Exception {
        long id = createProduct(bonjuk.admin(), "P-0001", "8801111111111");
        createProduct(bonjuk.admin(), "P-0002", "8802222222222");

        mockMvc.perform(put("/api/v1/products/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "김치만두 2kg", "barcode", "8801111111111",
                                "category", "냉동만두", "orderUnit", "EA", "unitPrice", 15000))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productCode").value("P-0001"))
                .andExpect(jsonPath("$.data.name").value("김치만두 2kg"))
                .andExpect(jsonPath("$.data.unitPrice").value(15000));

        // 타 상품이 쓰는 바코드로 변경 시도
        mockMvc.perform(put("/api/v1/products/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "김치만두 2kg", "barcode", "8802222222222",
                                "category", "냉동만두", "orderUnit", "EA", "unitPrice", 15000))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("BARCODE_DUPLICATED"));
    }

    @Test
    @DisplayName("판매중지 — 재중지는 409 CONFLICT (3.2.5)")
    void suspendProduct() throws Exception {
        long id = createProduct(bonjuk.admin(), "P-0001", "8801111111111");

        mockMvc.perform(post("/api/v1/products/" + id + "/suspend")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));

        mockMvc.perform(post("/api/v1/products/" + id + "/suspend")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    @DisplayName("목록 — keyword 검색·카테고리 필터·정렬·페이징 (3.2.2)")
    void listProducts() throws Exception {
        createProduct(bonjuk.admin(), "P-0001", "8801111111111");
        createProduct(bonjuk.admin(), "P-0002", "8802222222222");
        String token = bearer(bonjuk.admin());

        mockMvc.perform(get("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .param("keyword", "P-0002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].productCode").value("P-0002"));

        mockMvc.perform(get("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .param("category", "냉동식품")
                        .param("sort", "productCode,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].productCode").value("P-0001"))
                .andExpect(jsonPath("$.data.page.totalElements").value(2));

        // 허용되지 않은 정렬 필드
        mockMvc.perform(get("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .param("sort", "barcode,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("점주는 조회만 가능하다 — 목록 200, 등록 403 (3.2 접근 매트릭스)")
    void storeOwnerIsReadOnly() throws Exception {
        createProduct(bonjuk.admin(), "P-0001", "8801111111111");

        mockMvc.perform(get("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.owner())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1));

        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.owner()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productBody("P-0002", "8802222222222", 1000))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("HQ_MANAGER는 상품 쓰기가 가능하다 — 운영 업무 (3.2 접근 매트릭스)")
    void hqManagerCanWrite() throws Exception {
        User manager = User.registerHqManager(bonjuk.tenant().getId(), "manager@test.com",
                encodedPassword(), "김운영");
        manager.confirmPassword(encodedPassword());
        saveUser(manager);

        createProduct(manager, "P-0001", "8801111111111");
    }

    @Test
    @DisplayName("교차 테넌트 접근은 전부 404 — 조회·수정·중지 (NFR-2.4)")
    void crossTenantAccessIs404() throws Exception {
        long id = createProduct(bonjuk.admin(), "P-0001", "8801111111111");
        String otherToken = bearer(kimbap.admin());

        mockMvc.perform(get("/api/v1/products/" + id)
                        .header(HttpHeaders.AUTHORIZATION, otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(put("/api/v1/products/" + id)
                        .header(HttpHeaders.AUTHORIZATION, otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "탈취 시도", "barcode", "8800000000000",
                                "category", "기타", "orderUnit", "EA", "unitPrice", 1))))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/products/" + id + "/suspend")
                        .header(HttpHeaders.AUTHORIZATION, otherToken))
                .andExpect(status().isNotFound());

        // 목록에도 타 테넌트 상품이 보이지 않는다
        mockMvc.perform(get("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }
}
