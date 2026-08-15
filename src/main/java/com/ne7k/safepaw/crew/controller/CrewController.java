package com.ne7k.safepaw.crew.controller;

import com.ne7k.safepaw.crew.dto.request.CrewCreateRequest;
import com.ne7k.safepaw.crew.dto.request.CrewJoinRequest;
import com.ne7k.safepaw.crew.dto.request.CrewTransferRequest;
import com.ne7k.safepaw.crew.dto.request.CrewUpdateRequest;
import com.ne7k.safepaw.crew.dto.response.CrewMemberResponse;
import com.ne7k.safepaw.crew.dto.response.CrewResponse;
import com.ne7k.safepaw.crew.dto.response.CrewStatsResponse;
import com.ne7k.safepaw.crew.dto.response.CrewTerritoryResponse;
import com.ne7k.safepaw.crew.dto.response.CrewTerritoryUnionResponse;
import com.ne7k.safepaw.crew.service.CrewImageUploadService;
import com.ne7k.safepaw.crew.service.CrewService;
import com.ne7k.safepaw.crew.service.CrewTerritoryQueryService;
import com.ne7k.safepaw.dog.dto.request.MarkerUploadUrlRequest;
import com.ne7k.safepaw.dog.dto.response.MarkerUploadUrlResponse;
import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.global.response.ApiResponse;
import com.ne7k.safepaw.global.security.CustomUserDetails;
import com.ne7k.safepaw.territory.config.TerritoryProperties;
import com.ne7k.safepaw.walk.service.GeoUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Crews")
@RestController
@RequestMapping("/api/v1/crews")
@RequiredArgsConstructor
public class CrewController {

    private final CrewService crewService;
    private final CrewImageUploadService crewImageUploadService;
    private final CrewTerritoryQueryService crewTerritoryQueryService;
    private final TerritoryProperties territoryProperties;

    @PostMapping("/image/upload-url")
    @Operation(summary = "크루 이미지 업로드 URL 발급", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<MarkerUploadUrlResponse> uploadUrl(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody MarkerUploadUrlRequest req) {
        return ApiResponse.ok(
                crewImageUploadService.issueDraftUploadUrl(principal.getUserId(), req.contentType()));
    }

    @PostMapping
    @Operation(summary = "크루 생성", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<CrewResponse>> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CrewCreateRequest req) {
        CrewResponse body = crewService.create(principal.getUserId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(body));
    }

    @PostMapping("/join")
    @Operation(summary = "초대 코드로 크루 가입", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<CrewResponse> join(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CrewJoinRequest req) {
        return ApiResponse.ok(crewService.join(principal.getUserId(), req));
    }

    @GetMapping("/me")
    @Operation(summary = "내 크루", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<CrewResponse> me(@AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.ok(crewService.getMine(principal.getUserId()));
    }

    @GetMapping("/me/territories")
    @Operation(summary = "내 크루 영토 bbox", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<List<CrewTerritoryResponse>> myTerritories(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam double swLng, @RequestParam double swLat,
            @RequestParam double neLng, @RequestParam double neLat) {
        assertBbox(swLng, swLat, neLng, neLat);
        return ApiResponse.ok(crewTerritoryQueryService.findMineInBbox(
                principal.getUserId(), swLng, swLat, neLng, neLat));
    }

    @GetMapping("/{crewId}")
    @Operation(summary = "크루 공개 프로필", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<CrewResponse> detail(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long crewId) {
        return ApiResponse.ok(crewService.getPublic(principal.getUserId(), crewId));
    }

    @PatchMapping("/{crewId}")
    @Operation(summary = "크루 프로필 수정 (리더)", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<CrewResponse> update(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long crewId,
            @Valid @RequestBody CrewUpdateRequest req) {
        return ApiResponse.ok(crewService.update(principal.getUserId(), crewId, req));
    }

    @DeleteMapping("/{crewId}")
    @Operation(summary = "크루 해산 (리더)", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Void> disband(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long crewId) {
        crewService.disband(principal.getUserId(), crewId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{crewId}/leave")
    @Operation(summary = "크루 탈퇴", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Void> leave(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long crewId) {
        crewService.leave(principal.getUserId(), crewId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{crewId}/invite-code")
    @Operation(summary = "초대 코드 재발급 (리더)", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<CrewResponse> rotateInvite(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long crewId) {
        return ApiResponse.ok(crewService.rotateInviteCode(principal.getUserId(), crewId));
    }

    @PostMapping("/{crewId}/transfer")
    @Operation(summary = "리더 위임", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<CrewResponse> transfer(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long crewId,
            @Valid @RequestBody CrewTransferRequest req) {
        return ApiResponse.ok(crewService.transfer(principal.getUserId(), crewId, req));
    }

    @GetMapping("/{crewId}/members")
    @Operation(summary = "크루 멤버 목록", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<List<CrewMemberResponse>> members(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long crewId) {
        return ApiResponse.ok(crewService.listMembers(principal.getUserId(), crewId));
    }

    @DeleteMapping("/{crewId}/members/{userId}")
    @Operation(summary = "멤버 강퇴 (리더)", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Void> kick(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long crewId,
            @PathVariable Long userId) {
        crewService.kick(principal.getUserId(), crewId, userId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{crewId}/territories")
    @Operation(summary = "크루 영토 bbox", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<List<CrewTerritoryResponse>> territories(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long crewId,
            @RequestParam double swLng, @RequestParam double swLat,
            @RequestParam double neLng, @RequestParam double neLat) {
        assertBbox(swLng, swLat, neLng, neLat);
        return ApiResponse.ok(crewTerritoryQueryService.findInBbox(
                principal.getUserId(), crewId, swLng, swLat, neLng, neLat));
    }

    @GetMapping("/{crewId}/stats")
    @Operation(summary = "크루 시즌 영토 통계", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<CrewStatsResponse> stats(
            @PathVariable Long crewId,
            @RequestParam(required = false) String season) {
        return ApiResponse.ok(crewTerritoryQueryService.stats(crewId, season));
    }

    @GetMapping("/{crewId}/territory-union")
    @Operation(summary = "크루 영토 Union GeoJSON", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<CrewTerritoryUnionResponse> union(
            @PathVariable Long crewId,
            @RequestParam(required = false) String season) {
        return ApiResponse.ok(crewTerritoryQueryService.union(crewId, season));
    }

    private void assertBbox(double swLng, double swLat, double neLng, double neLat) {
        double widthM = GeoUtils.haversineMeters(swLng, swLat, neLng, swLat);
        double heightM = GeoUtils.haversineMeters(swLng, swLat, swLng, neLat);
        if (widthM * heightM > territoryProperties.bboxMaxAreaSquareMeters()) {
            throw new BusinessException(ErrorCode.TERRITORY_BBOX_TOO_LARGE);
        }
    }
}
