package com.orderflow.api.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * 카테고리 목록 API 통합 테스트 (US-CAT-01, api-spec 3.2.11) — 카테고리 칩 UI용.
 * 교차 테넌트 미노출은 2-테넌트 픽스처로 검증한다 (NFR-2.4).
 */
class ProductCategoryApiTest extends ApiIntegrationTest {

    private static final String CATEGORIES = "/api/v1/products/categories";

    private TenantSetup bonjuk;
    private TenantSetup kimbap;

    @BeforeEach
    void setUpTenants() {
        bonjuk = createTenantSetup("bonjuk");
        kimbap = createTenantSetup("kimbap");
    }

    private long createProduct(User user, String code, String barcode, String category) throws Exception {
        String response = mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productCode", code, "name", "테스트 상품", "barcode", barcode,
                                "category", category, "orderUnit", "BOX", "unitPrice", 10000))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private void suspend(User user, long productId) throws Exception {
        mockMvc.perform(post("/api/v1/products/" + productId + "/suspend")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("사용 중인 카테고리를 중복 없이 오름차순으로 반환한다 (3.2.11)")
    void listsUsedCategoriesSortedAndDistinct() throws Exception {
        createProduct(bonjuk.admin(), "P-0001", "8801111111111", "포장재");
        createProduct(bonjuk.admin(), "P-0002", "8801111111112", "냉동식품");
        createProduct(bonjuk.admin(), "P-0003", "8801111111113", "냉동식품"); // 중복 — 하나로 합쳐진다

        mockMvc.perform(get(CATEGORIES).header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0]").value("냉동식품"))
                .andExpect(jsonPath("$.data[1]").value("포장재"));
    }

    @Test
    @DisplayName("다른 테넌트의 카테고리는 섞이지 않는다 (NFR-2.4)")
    void doesNotLeakOtherTenantCategories() throws Exception {
        createProduct(bonjuk.admin(), "P-0001", "8801111111111", "냉동식품");
        createProduct(kimbap.admin(), "K-0001", "8802222222221", "김밥재료");

        mockMvc.perform(get(CATEGORIES).header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0]").value("냉동식품"));

        mockMvc.perform(get(CATEGORIES).header(HttpHeaders.AUTHORIZATION, bearer(kimbap.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0]").value("김밥재료"));
    }

    @Test
    @DisplayName("판매중지 상품만 남은 카테고리는 제외된다 — 칩을 눌렀을 때 빈 목록 방지 (3.2.11)")
    void excludesCategoriesWithoutOnSaleProducts() throws Exception {
        long suspended = createProduct(bonjuk.admin(), "P-0001", "8801111111111", "단종예정");
        createProduct(bonjuk.admin(), "P-0002", "8801111111112", "냉동식품");
        suspend(bonjuk.admin(), suspended);

        mockMvc.perform(get(CATEGORIES).header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0]").value("냉동식품"));
    }

    @Test
    @DisplayName("같은 카테고리에 판매중 상품이 하나라도 남아 있으면 유지된다 (3.2.11)")
    void keepsCategoryWhileAnyProductRemainsOnSale() throws Exception {
        long suspended = createProduct(bonjuk.admin(), "P-0001", "8801111111111", "냉동식품");
        createProduct(bonjuk.admin(), "P-0002", "8801111111112", "냉동식품");
        suspend(bonjuk.admin(), suspended);

        mockMvc.perform(get(CATEGORIES).header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0]").value("냉동식품"));
    }

    @Test
    @DisplayName("상품이 없으면 404가 아니라 빈 배열 (3.2.11)")
    void returnsEmptyArrayWhenNoProducts() throws Exception {
        mockMvc.perform(get(CATEGORIES).header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("점주도 조회할 수 있다 — 발주 화면에서 재사용 (3.2 접근 매트릭스)")
    void storeOwnerCanRead() throws Exception {
        createProduct(bonjuk.admin(), "P-0001", "8801111111111", "냉동식품");

        mockMvc.perform(get(CATEGORIES).header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.owner())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("냉동식품"));
    }

    /**
     * 회귀 방지 — {@code /products/categories}가 3.2.3의 {@code /products/{productId}}로 매칭되면
     * "categories"를 Long으로 변환하려다 400 INVALID_REQUEST가 나간다. 조용히 깨지는 경로라 고정해 둔다.
     */
    @Test
    @DisplayName("경로가 {productId}로 먹히지 않는다 — 400이 아니라 200 (3.2.11 vs 3.2.3)")
    void doesNotCollideWithProductIdPath() throws Exception {
        mockMvc.perform(get(CATEGORIES).header(HttpHeaders.AUTHORIZATION, bearer(bonjuk.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").doesNotExist());
    }
}
