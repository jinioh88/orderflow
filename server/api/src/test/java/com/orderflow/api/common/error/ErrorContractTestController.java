package com.orderflow.api.common.error;

import com.orderflow.api.common.response.ApiResponse;
import com.orderflow.common.error.BusinessException;
import com.orderflow.common.error.CommonErrorCode;
import com.orderflow.common.error.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 공통 규약(api-spec.md 1.3/1.4) 검증 전용 테스트 컨트롤러 — 프로덕션 코드가 아니다. */
@RestController
class ErrorContractTestController {

    record CreateRequest(@NotBlank(message = "필수 입력입니다.") String name,
                         @Min(value = 1, message = "1 이상이어야 합니다.") int quantity) {
    }

    @GetMapping("/test/success")
    ApiResponse<Map<String, String>> success() {
        return ApiResponse.of(Map.of("name", "김치만두"));
    }

    @PostMapping("/test/validated")
    ApiResponse<CreateRequest> validated(@Valid @RequestBody CreateRequest request) {
        return ApiResponse.of(request);
    }

    @GetMapping("/test/typed")
    ApiResponse<Integer> typed(@RequestParam int value) {
        return ApiResponse.of(value);
    }

    @GetMapping("/test/business")
    void business() {
        throw new BusinessException(CommonErrorCode.CONFLICT, "이미 마감된 회차입니다.");
    }

    @GetMapping("/test/not-found")
    void notFound() {
        throw new EntityNotFoundException();
    }

    @GetMapping("/test/unexpected")
    void unexpected() {
        throw new IllegalStateException("boom");
    }
}
