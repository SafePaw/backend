package com.ne7k.safepaw.dog.controller;

import com.ne7k.safepaw.dog.dto.request.DogCreateRequest;
import com.ne7k.safepaw.dog.dto.request.DogUpdateRequest;
import com.ne7k.safepaw.dog.dto.request.MarkerUploadUrlRequest;
import com.ne7k.safepaw.dog.dto.response.DogResponse;
import com.ne7k.safepaw.dog.dto.response.MarkerUploadUrlResponse;
import com.ne7k.safepaw.dog.service.DogService;
import com.ne7k.safepaw.dog.service.MarkerUploadService;
import com.ne7k.safepaw.global.response.ApiResponse;
import com.ne7k.safepaw.global.security.CustomUserDetails;
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
@Tag(name = "Dogs")
@RestController
@RequestMapping("/api/v1/dogs")
@RequiredArgsConstructor
public class DogController {

    private final DogService dogService;
    private final MarkerUploadService markerUploadService;

    @GetMapping
    @Operation(summary = "내 강아지 목록", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<List<DogResponse>> list(@AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.ok(dogService.listMine(principal.getUserId()));
    }

    @PostMapping
    @Operation(summary = "강아지 등록", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<DogResponse>> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody DogCreateRequest req
    ) {
        DogResponse body = dogService.create(principal.getUserId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(body));
    }

    @PostMapping("/marker/upload-url")
    @Operation(summary = "마커 업로드 URL 발급", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<MarkerUploadUrlResponse> uploadUrl(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody MarkerUploadUrlRequest req
    ) {
        return ApiResponse.ok(markerUploadService.issueDraftUploadUrl(principal.getUserId(), req.contentType()));
    }

    @PatchMapping("/{dogId}")
    @Operation(summary = "강아지 프로필 수정", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<DogResponse> update(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long dogId,
            @Valid @RequestBody DogUpdateRequest req
    ) {
        return ApiResponse.ok(dogService.update(principal.getUserId(), dogId, req));
    }

    @DeleteMapping("/{dogId}")
    @Operation(summary = "강아지 삭제", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long dogId
    ) {
        dogService.delete(principal.getUserId(), dogId);
        return ApiResponse.ok(null);
    }
}
