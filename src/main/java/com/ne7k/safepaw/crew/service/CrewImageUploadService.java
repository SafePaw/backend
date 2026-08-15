package com.ne7k.safepaw.crew.service;

import com.ne7k.safepaw.dog.dto.response.MarkerUploadUrlResponse;
import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.global.storage.MinioStorageClient;
import com.ne7k.safepaw.global.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrewImageUploadService {

    private static final Duration UPLOAD_TTL = Duration.ofMinutes(10);
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");

    private final MinioStorageClient storage;
    private final StorageProperties storageProperties;

    public MarkerUploadUrlResponse issueDraftUploadUrl(Long userId, String contentType) {
        if (!ALLOWED.contains(contentType)) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "지원하지 않는 이미지 형식입니다.");
        }
        String ext = switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
        String key = "crews/" + userId + "/draft/" + UUID.randomUUID() + "." + ext;
        String bucket = storageProperties.bucketMarkers();
        String uploadUrl = storage.presignPut(bucket, key, contentType, UPLOAD_TTL);
        return new MarkerUploadUrlResponse(uploadUrl, key, (int) UPLOAD_TTL.toSeconds());
    }
}
