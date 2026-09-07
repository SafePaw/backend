package com.ne7k.safepaw.walk.dto.response;

import java.time.OffsetDateTime;

public record ShareCardResponse(
        Long id,
        Long walkId,
        String backgroundImageUrl,
        String renderedImageUrl,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
