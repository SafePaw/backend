package com.ne7k.safepaw.walk.controller;
import com.ne7k.safepaw.global.response.ApiResponse;
import com.ne7k.safepaw.global.security.CustomUserDetails;
import com.ne7k.safepaw.walk.dto.request.*;
import com.ne7k.safepaw.walk.dto.response.*;
import com.ne7k.safepaw.walk.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Walks")
@RestController
@RequestMapping("/api/v1/walks")
@RequiredArgsConstructor
public class WalkController {

    private final WalkSessionService walkSessionService;
    private final WalkPointAppendService appendService;
    private final WalkLiveService walkLiveService;
    private final WalkPauseService walkPauseService;

    @PostMapping
    @Operation(summary = "산책 시작", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<WalkStartResponse>> start(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody WalkStartRequest req) {
        WalkStartResponse body = walkSessionService.start(principal.getUserId(), req.dogId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(body));
    }

    /** set7: 활성 산책 복구용 — /{walkId} 보다 위에 둘 것 */
    @GetMapping("/active")
    @Operation(summary = "내 활성 산책 목록 (ONGOING|PAUSED)", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<ActiveWalkListResponse> active(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) Long dogId) {
        return ApiResponse.ok(walkSessionService.listActive(principal.getUserId(), dogId));
    }

    @PostMapping("/{walkId}/points")
    @Operation(summary = "GPS 배치 업로드", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> points(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long walkId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody WalkPointBatchRequest req) {
        appendService.append(principal.getUserId(), walkId, req.points());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(null));
    }

    @PostMapping("/{walkId}/finish")
    @Operation(summary = "산책 종료(영토 계산)", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<WalkFinishResponse> finish(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long walkId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody WalkFinishRequest req) {
        return ApiResponse.ok(walkSessionService.finish(principal.getUserId(), walkId, req.lastPoints()));
    }

    @PostMapping("/{walkId}/abort")
    @Operation(summary = "산책 중도 포기", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Void> abort(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long walkId) {
        walkSessionService.abort(principal.getUserId(), walkId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{walkId}")
    @Operation(summary = "산책 상세/경로", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<WalkDetailResponse> detail(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long walkId) {
        return ApiResponse.ok(walkSessionService.detail(principal.getUserId(), walkId));
    }

    @GetMapping("/{walkId}/live")
    @Operation(summary = "산책 실시간 통계", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<WalkLiveResponse> live(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long walkId) {
        return ApiResponse.ok(walkLiveService.live(principal.getUserId(), walkId));
    }

    @PostMapping("/{walkId}/pause")
    @Operation(summary = "산책 일시정지", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<WalkPauseResponse> pause(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long walkId) {
        return ApiResponse.ok(walkPauseService.pause(principal.getUserId(), walkId));
    }

    @PostMapping("/{walkId}/resume")
    @Operation(summary = "산책 재개", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<WalkResumeResponse> resume(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long walkId) {
        return ApiResponse.ok(walkPauseService.resume(principal.getUserId(), walkId));
    }
}