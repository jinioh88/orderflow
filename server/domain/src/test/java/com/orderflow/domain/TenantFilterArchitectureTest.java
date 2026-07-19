package com.orderflow.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.orderflow.domain.common.TenantFilter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.hibernate.annotations.Filter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * 격리 설계 노트 §5의 구조적 가드 — 우회 경로 #6(@Filter 누락 엔티티), #3(교차 애그리거트 to-one).
 * 신규 엔티티가 추가되면 자동으로 이 그물에 걸린다.
 */
class TenantFilterArchitectureTest {

    private static final String DOMAIN_PACKAGE = "com.orderflow.domain";

    @Test
    @DisplayName("tenant_id 컬럼을 가진 모든 엔티티는 tenantFilter를 선언해야 한다 (우회 경로 #6)")
    void tenantScopedEntitiesMustDeclareFilter() {
        for (Class<?> entity : scanEntities()) {
            if (!hasTenantIdColumn(entity)) {
                continue;
            }
            boolean declaresFilter = Arrays.stream(entity.getAnnotationsByType(Filter.class))
                    .anyMatch(f -> TenantFilter.NAME.equals(f.name()));
            assertThat(declaresFilter)
                    .as("%s는 tenant_id를 갖지만 @Filter(name = tenantFilter) 선언이 없다 — "
                            + "@MappedSuperclass로는 상속되지 않으므로 엔티티에 직접 선언해야 한다", entity.getName())
                    .isTrue();
        }
    }

    @Test
    @DisplayName("to-one 연관은 같은 애그리거트 패키지 안에서만 허용된다 (우회 경로 #3 — 프록시 로딩은 필터를 우회)")
    void toOneAssociationsMustStayInsideAggregatePackage() {
        for (Class<?> entity : scanEntities()) {
            for (Field field : entity.getDeclaredFields()) {
                if (!field.isAnnotationPresent(ManyToOne.class) && !field.isAnnotationPresent(OneToOne.class)) {
                    continue;
                }
                assertThat(field.getType().getPackageName())
                        .as("%s.%s — 애그리거트/컨텍스트 경계를 넘는 참조는 객체 연관이 아니라 ID로만 한다 (04 §1)",
                                entity.getSimpleName(), field.getName())
                        .isEqualTo(entity.getPackageName());
            }
        }
    }

    private List<Class<?>> scanEntities() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        List<Class<?>> entities = scanner.findCandidateComponents(DOMAIN_PACKAGE).stream()
                .map(bd -> {
                    try {
                        return Class.forName(bd.getBeanClassName());
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .<Class<?>>map(c -> c)
                .toList();
        assertThat(entities).as("엔티티 스캔이 비어 있으면 이 테스트는 아무것도 지키지 못한다").isNotEmpty();
        return entities;
    }

    private boolean hasTenantIdColumn(Class<?> entity) {
        return Arrays.stream(entity.getDeclaredFields()).anyMatch(f -> {
            Column column = f.getAnnotation(Column.class);
            return "tenantId".equals(f.getName())
                    || (column != null && "tenant_id".equals(column.name()));
        });
    }
}
