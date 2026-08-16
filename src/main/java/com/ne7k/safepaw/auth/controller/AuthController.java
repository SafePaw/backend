package com.ne7k.safepaw.auth.controller;

import com.ne7k.safepaw.auth.dto.request.SocialLoginRequest;
import com.ne7k.safepaw.auth.dto.request.TokenRefreshRequest;
import com.ne7k.safepaw.auth.dto.response.AuthTokenResponse;
import com.ne7k.safepaw.auth.service.RefreshTokenService;
import com.ne7k.safepaw.auth.service.SocialAuthService;
import com.ne7k.safepaw.global.response.ApiResponse;
import com.ne7k.safepaw.global.security.CustomUserDetails;
import com.ne7k.safepaw.notification.service.DeviceTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag( name = "Auth", description = "소셜 로그인") // swagger
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final SocialAuthService socialAuthService;
    private final RefreshTokenService refreshTokenService;
    private final DeviceTokenService deviceTokenService;

    @Operation(summary = "소셜 로그인") // swagger
    @PostMapping("/social")
    public ApiResponse<AuthTokenResponse> social(
            @Valid @RequestBody SocialLoginRequest req, // Valid dto 내부 notnull 검증
            HttpServletRequest httpReq
    ) {
        String userAgent = httpReq.getHeader("User-Agent"); // 요청 헤더 문자열
        return ApiResponse.ok(socialAuthService.loginOrSignup(req, userAgent));
    }

    @Operation(summary = "토큰 재발급")
    @PostMapping("/refresh")
    public ApiResponse<AuthTokenResponse> refresh(
            @Valid @RequestBody TokenRefreshRequest req,
            HttpServletRequest httpReq
    ) {
        String userAgent = httpReq.getHeader("User-Agent");
        var result = refreshTokenService.rotate(req.refreshToken(), userAgent);
        return ApiResponse.ok(new AuthTokenResponse(
                result.accessToken(),
                result.refreshToken(),
                result.user()
        ));

    }

    @Operation(summary = "로그아웃 - refresh token 폐기",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal CustomUserDetails principal) {
        refreshTokenService.revokeAll(principal.getUserId());
        deviceTokenService.unregisterAll(principal.getUserId());
        return ApiResponse.ok(null);
    }

}
