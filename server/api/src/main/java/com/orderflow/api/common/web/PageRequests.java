package com.orderflow.api.common.web;

import com.orderflow.common.error.BusinessException;
import com.orderflow.common.error.CommonErrorCode;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 공통 페이징 규약 (api-spec 1.5) — size 최대 100, 정렬 허용 필드는 API별 화이트리스트.
 */
public final class PageRequests {

    private static final int MAX_SIZE = 100;

    private PageRequests() {
    }

    /** size 상한 적용 + 정렬 필드 화이트리스트 검증. 미정렬이면 defaultSort 사용. */
    public static Pageable resolve(Pageable pageable, Set<String> allowedSortFields, Sort defaultSort) {
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : defaultSort;
        sort.forEach(order -> {
            if (!allowedSortFields.contains(order.getProperty())) {
                throw new BusinessException(CommonErrorCode.INVALID_REQUEST,
                        "허용되지 않은 정렬 필드입니다: " + order.getProperty());
            }
        });
        return PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), MAX_SIZE), sort);
    }
}
