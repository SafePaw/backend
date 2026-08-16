package com.ne7k.safepaw.ranking.controller;

import com.ne7k.safepaw.crew.dto.response.CrewRankingBoardResponse;
import com.ne7k.safepaw.crew.dto.response.MyCrewRankingResponse;
import com.ne7k.safepaw.crew.service.CrewRankingQueryService;
import com.ne7k.safepaw.global.response.ApiResponse;
import com.ne7k.safepaw.global.security.CustomUserDetails;
import com.ne7k.safepaw.ranking.domain.RankingCategory;
import com.ne7k.safepaw.ranking.dto.response.MyRankingResponse;
import com.ne7k.safepaw.ranking.dto.response.RankingBoardResponse;
import com.ne7k.safepaw.ranking.service.RankingQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Rankings")
@RestController
@RequestMapping("/api/v1/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final RankingQueryService rankingQueryService;
    private final CrewRankingQueryService crewRankingQueryService;

    @GetMapping("/xp")
    @Operation(summary = "시즌 XP 랭킹 (메인)", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<RankingBoardResponse> xp(
            @RequestParam(required = false) String season,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(rankingQueryService.board(season, RankingCategory.XP, page, size));
    }

    @GetMapping("/territory")
    @Operation(summary = "시즌 영토 면적 랭킹", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<RankingBoardResponse> territory(
            @RequestParam(required = false) String season,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(rankingQueryService.board(season, RankingCategory.TERRITORY, page, size));
    }

    @GetMapping("/distance")
    @Operation(summary = "시즌 누적 거리 랭킹", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<RankingBoardResponse> distance(
            @RequestParam(required = false) String season,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(rankingQueryService.board(season, RankingCategory.DISTANCE, page, size));
    }

    @GetMapping("/duration")
    @Operation(summary = "시즌 누적 시간 랭킹", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<RankingBoardResponse> duration(
            @RequestParam(required = false) String season,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(rankingQueryService.board(season, RankingCategory.DURATION, page, size));
    }

    @GetMapping("/me")
    @Operation(summary = "내 강아지 시즌 순위", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<MyRankingResponse> me(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) String season,
            @RequestParam Long dogId) {
        return ApiResponse.ok(rankingQueryService.me(principal.getUserId(), season, dogId));
    }

    @GetMapping("/crew-territory")
    @Operation(summary = "시즌 크루 영토 면적 랭킹", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<CrewRankingBoardResponse> crewTerritory(
            @RequestParam(required = false) String season,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(crewRankingQueryService.board(season, page, size));
    }

    @GetMapping("/crew-territory/me")
    @Operation(summary = "내 크루 시즌 영토 순위", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<MyCrewRankingResponse> myCrewTerritory(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) String season) {
        return ApiResponse.ok(crewRankingQueryService.me(principal.getUserId(), season));
    }
}
