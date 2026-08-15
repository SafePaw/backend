package com.ne7k.safepaw.notification.controller;

import com.ne7k.safepaw.global.response.ApiResponse;
import com.ne7k.safepaw.global.security.CustomUserDetails;
import com.ne7k.safepaw.notification.dto.request.DeviceTokenRegisterRequest;
import com.ne7k.safepaw.notification.dto.request.DeviceTokenUnregisterRequest;
import com.ne7k.safepaw.notification.service.DeviceTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "DeviceTokens")
@RestController
@RequestMapping("/api/v1/device-tokens")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping
    @Operation(summary = "FCM 토큰 등록/갱신", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Void> register(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody DeviceTokenRegisterRequest req
    ) {
        deviceTokenService.register(principal.getUserId(), req);
        return ApiResponse.ok(null);
    }

    @DeleteMapping
    @Operation(summary = "FCM 토큰 삭제", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Void> unregister(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody DeviceTokenUnregisterRequest req
    ) {
        deviceTokenService.unregister(req.token());
        return ApiResponse.ok(null);
    }
}