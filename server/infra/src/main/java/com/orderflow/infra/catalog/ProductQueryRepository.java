package com.orderflow.infra.catalog;

import static com.orderflow.domain.catalog.QHqStock.hqStock;
import static com.orderflow.domain.catalog.QProduct.product;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * 상품 목록 조회 (api-spec 3.2.2) — CQRS-lite QueryDSL DTO 프로젝션 (방법론 6).
 * 엑셀 다운로드(3.3.3)도 이 조건을 재사용한다. 테넌트 스코프는 Hibernate 필터가
 * 루트·조인 엔티티 모두에 강제한다 (JPQL 기반이므로 @Filter 적용).
 */
@Repository
public class ProductQueryRepository {

    private final JPAQueryFactory queryFactory;

    public ProductQueryRepository(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    public Page<ProductSummary> search(ProductSearchCondition condition, Pageable pageable) {
        List<ProductSummary> items = baseQuery(condition)
                .orderBy(toOrderSpecifiers(pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory.select(product.count())
                .from(product)
                .where(predicates(condition))
                .fetchOne();
        return new PageImpl<>(items, pageable, total == null ? 0 : total);
    }

    /** 엑셀 다운로드용 전체 조회 — 페이징 없음, 품목코드 오름차순 고정 (api-spec 3.3.3) */
    public List<ProductSummary> findAllForExcel(ProductSearchCondition condition) {
        return baseQuery(condition)
                .orderBy(product.productCode.asc())
                .fetch();
    }

    private JPAQuery<ProductSummary> baseQuery(ProductSearchCondition condition) {
        return queryFactory
                .select(Projections.constructor(ProductSummary.class,
                        product.id, product.productCode, product.name, product.barcode,
                        product.category, product.orderUnit, product.unitPrice,
                        product.limited, hqStock.availableQty, product.status, product.createdAt))
                .from(product)
                .leftJoin(hqStock).on(hqStock.productId.eq(product.id))
                .where(predicates(condition));
    }

    private BooleanExpression[] predicates(ProductSearchCondition condition) {
        return new BooleanExpression[]{
                keywordMatches(condition.keyword()),
                condition.category() == null ? null : product.category.eq(condition.category()),
                condition.status() == null ? null : product.status.eq(condition.status()),
                condition.limited() == null ? null : product.limited.eq(condition.limited())
        };
    }

    /** 품목코드·품명·바코드 부분 일치 (api-spec 3.2.2) */
    private BooleanExpression keywordMatches(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return product.productCode.contains(keyword)
                .or(product.name.contains(keyword))
                .or(product.barcode.contains(keyword));
    }

    /** 정렬 필드 화이트리스트는 컨트롤러(PageRequests)가 이미 검증했다 */
    private OrderSpecifier<?>[] toOrderSpecifiers(Sort sort) {
        return sort.stream()
                .map(order -> {
                    ComparableExpressionBase<?> path = switch (order.getProperty()) {
                        case "productCode" -> product.productCode;
                        case "name" -> product.name;
                        case "unitPrice" -> product.unitPrice;
                        default -> product.createdAt;
                    };
                    return order.isAscending() ? path.asc() : path.desc();
                })
                .toArray(OrderSpecifier[]::new);
    }
}
