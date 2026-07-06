package com.ne7k.safepaw.walk.repository.redis;

import java.time.OffsetDateTime;
import java.util.Map;

/** Redis Hash → 값 객체 */
public record WalkState(
        long userId,
        long dogId,
        OffsetDateTime startedAt,
        Double lastLng,
        Double lastLat,
        OffsetDateTime lastAt,
        int pointCount,
        double totalMeters
) {
    public boolean hasLast() { return lastLng != null && lastLat != null && lastAt != null; }

    public static WalkState from(Map<Object, Object> h) {
        return new WalkState(
                Long.parseLong((String) h.get("userId")),
                Long.parseLong((String) h.get("dogId")),
                OffsetDateTime.parse((String) h.get("startedAt")),
                h.get("lastLng") == null ? null : Double.parseDouble((String) h.get("lastLng")),
                h.get("lastLat") == null ? null : Double.parseDouble((String) h.get("lastLat")),
                h.get("lastAt") == null ? null : OffsetDateTime.parse((String) h.get("lastAt")),
                Integer.parseInt((String) h.getOrDefault("pointCount", "0")),
                Double.parseDouble((String) h.getOrDefault("totalMeters", "0"))
        );
    }
}
