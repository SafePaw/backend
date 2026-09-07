package com.ne7k.safepaw.walk.service;

import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.global.storage.MinioStorageClient;
import com.ne7k.safepaw.global.storage.StorageProperties;
import com.ne7k.safepaw.walk.domain.WalkShareCard;
import com.ne7k.safepaw.walk.domain.WalkSession;
import com.ne7k.safepaw.walk.domain.WalkStatus;
import com.ne7k.safepaw.walk.dto.request.ShareCardRequest;
import com.ne7k.safepaw.walk.dto.response.ShareCardResponse;
import com.ne7k.safepaw.walk.dto.response.ShareCardUploadUrlResponse;
import com.ne7k.safepaw.walk.repository.WalkSessionRepository;
import com.ne7k.safepaw.walk.repository.WalkShareCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalkShareCardService {

    private static final int UPLOAD_URL_TTL_SECONDS = 300;

    private final WalkSessionRepository walkSessionRepository;
    private final WalkShareCardRepository shareCardRepository;
    private final MinioStorageClient storageClient;
    private final StorageProperties storageProperties;

    /** 이미지 presigned PUT URL 발급 */
    public ShareCardUploadUrlResponse uploadUrl(Long userId, Long walkId,
                                                String type, String contentType) {
        walkSessionRepository.findByIdAndDog_Owner_Id(walkId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALK_NOT_FOUND));

        String ext = contentTypeToExt(contentType);
        String prefix = "BACKGROUND".equalsIgnoreCase(type) ? "bg" : "card";
        String key = "walk-share/%d/%d/%s-%s.%s"
                .formatted(userId, walkId, prefix, UUID.randomUUID(), ext);

        String uploadUrl = storageClient.presignPut(
                storageProperties.bucketShareCards(),
                key,
                contentType,
                Duration.ofSeconds(UPLOAD_URL_TTL_SECONDS));

        return new ShareCardUploadUrlResponse(uploadUrl, key, UPLOAD_URL_TTL_SECONDS);
    }

    /** 공유 카드 저장/갱신 (UPSERT) */
    @Transactional
    public ShareCardResponse save(Long userId, Long walkId, ShareCardRequest req) {
        WalkSession session = walkSessionRepository.findByIdAndDog_Owner_Id(walkId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALK_NOT_FOUND));

        if (session.getStatus() != WalkStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.WALK_NOT_COMPLETED);
        }

        WalkShareCard card = shareCardRepository.findByWalkSession_Id(walkId)
                .map(existing -> {
                    existing.update(req.backgroundImageKey(), req.renderedImageKey());
                    return existing;
                })
                .orElseGet(() -> shareCardRepository.save(
                        WalkShareCard.create(session, req.backgroundImageKey(), req.renderedImageKey())));

        return toResponse(card);
    }

    /** 공유 카드 조회 */
    @Transactional(readOnly = true)
    public ShareCardResponse get(Long userId, Long walkId) {
        walkSessionRepository.findByIdAndDog_Owner_Id(walkId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALK_NOT_FOUND));

        WalkShareCard card = shareCardRepository.findByWalkSession_Id(walkId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHARE_CARD_NOT_FOUND));

        return toResponse(card);
    }

    private ShareCardResponse toResponse(WalkShareCard card) {
        String bgUrl = buildUrl(card.getBackgroundImageKey());
        String renderedUrl = buildUrl(card.getRenderedImageKey());
        return new ShareCardResponse(
                card.getId(),
                card.getWalkSession().getId(),
                bgUrl,
                renderedUrl,
                card.getCreatedAt(),
                card.getUpdatedAt()
        );
    }

    private String buildUrl(String key) {
        if (key == null || key.isBlank()) return null;
        return storageClient.publicUrl(storageProperties.bucketShareCards(), key);
    }

    private static String contentTypeToExt(String contentType) {
        if (contentType == null) return "jpg";
        return switch (contentType.toLowerCase()) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> "jpg";
        };
    }
}
