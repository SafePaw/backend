package com.ne7k.safepaw.user.controller;

import com.ne7k.safepaw.global.response.ApiResponse;
import com.ne7k.safepaw.global.security.CustomUserDetails;
import com.ne7k.safepaw.user.dto.request.UserUpdateRequest;
import com.ne7k.safepaw.user.dto.response.MeResponse;
import com.ne7k.safepaw.user.service.UserQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Me")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class UserController {

    private final UserQueryService userQueryService;

    @GetMapping
    @Operation(summary = "내 프로필", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<MeResponse> me(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ApiResponse.ok(userQueryService.getMe(principal.getUserId()));
    }

    @PatchMapping
    @Operation(summary = "닉네임 수정", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<MeResponse> updateNickname(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UserUpdateRequest req
    ) {
        return ApiResponse.ok(userQueryService.updateNickname(principal.getUserId(), req.nickname()));
    }

}
