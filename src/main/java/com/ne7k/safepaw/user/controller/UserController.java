package com.ne7k.safepaw.user.controller;

import com.ne7k.safepaw.global.response.ApiResponse;
import com.ne7k.safepaw.global.security.CustomUserDetails;
import com.ne7k.safepaw.territory.service.TerritoryService;
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

import com.ne7k.safepaw.global.response.PageResponse;
import com.ne7k.safepaw.territory.domain.Territory;
import com.ne7k.safepaw.territory.domain.TerritoryStatus;
import com.ne7k.safepaw.territory.dto.response.TerritoryResponse;
import com.ne7k.safepaw.territory.repository.TerritoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.transaction.annotation.Transactional;

@Tag(name = "Me")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class UserController {

    private final TerritoryRepository territoryRepository;
    private final TerritoryService territoryService;
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

    @Transactional(readOnly = true)
    @GetMapping("/territories")
    @Operation(summary = "내 강아지들의 영토 목록", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<PageResponse<TerritoryResponse>> myTerritories(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) Long dogId,
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        TerritoryStatus ts = TerritoryStatus.valueOf(status);
        Pageable pageable = PageRequest.of(page, size, Sort.by("claimedAt").descending());
        Long userId = principal.getUserId();

        Page<Territory> result = (dogId != null)
                ? territoryRepository.findByDog_Owner_IdAndDog_IdAndStatus(userId, dogId, ts, pageable)
                : territoryRepository.findByDog_Owner_IdAndStatus(userId, ts, pageable);

        return ApiResponse.ok(
                PageResponse.from(result.map(t -> territoryService.toResponse(t, userId)))
        );
    }
}
