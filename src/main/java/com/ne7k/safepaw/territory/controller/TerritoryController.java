package com.ne7k.safepaw.territory.controller;

import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.global.response.ApiResponse;
import com.ne7k.safepaw.global.security.CustomUserDetails;
import com.ne7k.safepaw.territory.config.TerritoryProperties;
import com.ne7k.safepaw.territory.dto.response.TerritoryResponse;
import com.ne7k.safepaw.territory.service.TerritoryService;
import com.ne7k.safepaw.walk.service.GeoUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.ne7k.safepaw.territory.domain.Territory;

@Tag(name = "Territories")
@RestController
@RequestMapping("/api/v1/territories")
@RequiredArgsConstructor
public class TerritoryController {

    private final TerritoryService territoryService;
    private final TerritoryProperties props;

    @GetMapping
    @Operation(summary = "지도 bbox 영토 조회", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<List<TerritoryResponse>> inBbox(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam double swLng, @RequestParam double swLat,
            @RequestParam double neLng, @RequestParam double neLat) {

        // bbox 면적 가드 (대각선 두 변 곱 근사)
        double widthM = GeoUtils.haversineMeters(swLng, swLat, neLng, swLat);
        double heightM = GeoUtils.haversineMeters(swLng, swLat, swLng, neLat);
        if (widthM * heightM > props.bboxMaxAreaSquareMeters()) {
            throw new BusinessException(ErrorCode.TERRITORY_BBOX_TOO_LARGE);
        }

        var list = territoryService.findInBbox(swLng, swLat, neLng, neLat).stream()
                .map(t -> TerritoryResponse.from(t, principal.getUserId()))
                .toList();
        return ApiResponse.ok(list);
    }

    @GetMapping("/{territoryId}")
    @Operation(summary = "영토 상세", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<TerritoryResponse> detail(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long territoryId) {
        Territory territory = territoryService.findById(territoryId);
        return ApiResponse.ok(TerritoryResponse.from(territory, principal.getUserId()));
    }
}