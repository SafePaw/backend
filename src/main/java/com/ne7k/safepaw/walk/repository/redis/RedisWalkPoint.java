package com.ne7k.safepaw.walk.repository.redis;

import java.time.OffsetDateTime;

public record RedisWalkPoint(
        double lng,
        double lat,
        float accuracyMeters,
        Float speedKmh,
        OffsetDateTime recordedAt
) {
}
