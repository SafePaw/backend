package com.ne7k.safepaw.global.Controller;

import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController // json만 뱉어낸다고 명시
@RequestMapping("/api/v1/ping") // 공통 주소
public class HealthController {

    // 200 test
    @GetMapping
    public ApiResponse<Map<String, String>> ping() {
        return ApiResponse.ok(Map.of("status", "ok"));
    }

    @GetMapping("/error-test")
    public ApiResponse<Void> errorTest() {
        throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
    }

    @PostMapping("/validate-test")
    public ApiResponse<Void> validationTest(@Valid pingRequest request) {
        return ApiResponse.ok(null);
    }

    public record pingRequest(@NotBlank String name) {}
}
