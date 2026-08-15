package com.ne7k.safepaw.user.controller;

import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.global.response.ApiResponse;
import com.ne7k.safepaw.global.response.PageResponse;
import com.ne7k.safepaw.global.security.CustomUserDetails;
import com.ne7k.safepaw.territory.domain.Territory;
import com.ne7k.safepaw.territory.domain.TerritoryStatus;
import com.ne7k.safepaw.territory.dto.response.TerritoryResponse;
import com.ne7k.safepaw.territory.repository.TerritoryRepository;
import com.ne7k.safepaw.territory.service.TerritoryService;
import com.ne7k.safepaw.user.dto.request.UserUpdateRequest;
import com.ne7k.safepaw.user.dto.response.MeResponse;
import com.ne7k.safepaw.user.service.UserQueryService;
import com.ne7k.safepaw.walk.domain.WalkStatus;
import com.ne7k.safepaw.walk.dto.response.WalkHistoryItemResponse;
import com.ne7k.safepaw.walk.service.WalkSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Tag(name = "Me")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class UserController {

    private static final Set<WalkStatus> ALLOWED_HISTORY_STATUSES =
            EnumSet.of(WalkStatus.COMPLETED, WalkStatus.ABORTED);

    private final TerritoryRepository territoryRepository;
    private final TerritoryService territoryService;
    private final UserQueryService userQueryService;
    private final WalkSessionService walkSessionService;

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

    /** set10: 산책 기록 목록 */
    @Transactional(readOnly = true)
    @GetMapping("/walks")
    @Operation(summary = "내 산책 기록 목록", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<PageResponse<WalkHistoryItemResponse>> myWalks(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) Long dogId,
            @RequestParam(required = false) String walkType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<WalkStatus> statuses = parseHistoryStatuses(status);
        Boolean territoryOnly = resolveTerritoryOnly(walkType);

        var result = walkSessionService.listHistory(
                principal.getUserId(),
                dogId,
                territoryOnly,
                statuses,
                page,
                size
        );

        return ApiResponse.ok(PageResponse.from(result));
    }

    private static List<WalkStatus> parseHistoryStatuses(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.copyOf(ALLOWED_HISTORY_STATUSES);
        }
        try {
            List<WalkStatus> parsed = Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(WalkStatus::valueOf)
                    .distinct()
                    .toList();
            if (parsed.isEmpty() || !ALLOWED_HISTORY_STATUSES.containsAll(parsed)) {
                throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "status");
            }
            return parsed;
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "status");
        }
    }

    private static Boolean resolveTerritoryOnly(String walkType) {
        if (walkType == null || walkType.isBlank()) {
            return null;
        }
        if ("TERRITORY".equalsIgnoreCase(walkType.trim())) {
            return true;
        }
        if ("NORMAL".equalsIgnoreCase(walkType.trim())) {
            return false;
        }
        throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "walkType");
    }
}