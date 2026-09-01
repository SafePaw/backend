package com.ne7k.safepaw.walk.repository.redis;

import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.walk.config.WalkProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class WalkSessionStateCache {

    private final StringRedisTemplate redis;
    private final WalkProperties walkProperties;

    private String key(long walkId) { return "walk:state:" + walkId; }

    private Duration ttl() {
        return Duration.ofHours(walkProperties.session().bufferTtlHours());
    }

    public void init(long walkId, long userId, long dogId, OffsetDateTime startedAt) {
        Map<String, String> h = new HashMap<>();
        h.put("userId", String.valueOf(userId));
        h.put("dogId", String.valueOf(dogId));
        h.put("startedAt", startedAt.toString());
        h.put("pointCount", "0");
        h.put("totalMeters", "0");
        h.put("totalPausedSeconds", "0");
        redis.opsForHash().putAll(key(walkId), h);
        redis.expire(key(walkId), ttl());
    }

    public WalkState get(long walkId) {
        Map<Object, Object> h = redis.opsForHash().entries(key(walkId));
        if (h.isEmpty()) throw new BusinessException(ErrorCode.WALK_NOT_ONGOING);
        return WalkState.from(h);
    }

    /** 배치 검증 후: prev ← last, last ← 새 포인트, bbox 갱신 */
    public void updateAfterBatch(long walkId, RedisWalkPoint newLast,
                                 double addedMeters, int addedCount) {
        String k = key(walkId);
        Map<Object, Object> current = redis.opsForHash().entries(k);

        if (current.get("lastLng") != null) {
            redis.opsForHash().put(k, "prevLng", (String) current.get("lastLng"));
            redis.opsForHash().put(k, "prevLat", (String) current.get("lastLat"));
            redis.opsForHash().put(k, "prevAt",  (String) current.get("lastAt"));
        }

        redis.opsForHash().put(k, "lastLng", String.valueOf(newLast.lng()));
        redis.opsForHash().put(k, "lastLat", String.valueOf(newLast.lat()));
        redis.opsForHash().put(k, "lastAt",  newLast.recordedAt().toString());

        redis.opsForHash().increment(k, "pointCount", addedCount);
        double oldTotal = parseDoubleOr(current, "totalMeters", 0.0);
        redis.opsForHash().put(k, "totalMeters", String.valueOf(oldTotal + addedMeters));

        updateBbox(k, current, newLast.lng(), newLast.lat());
    }

    private void updateBbox(String k, Map<Object, Object> current, double lng, double lat) {
        double minLng = parseDoubleOr(current, "minLng", lng);
        double maxLng = parseDoubleOr(current, "maxLng", lng);
        double minLat = parseDoubleOr(current, "minLat", lat);
        double maxLat = parseDoubleOr(current, "maxLat", lat);

        redis.opsForHash().put(k, "minLng", String.valueOf(Math.min(minLng, lng)));
        redis.opsForHash().put(k, "maxLng", String.valueOf(Math.max(maxLng, lng)));
        redis.opsForHash().put(k, "minLat", String.valueOf(Math.min(minLat, lat)));
        redis.opsForHash().put(k, "maxLat", String.valueOf(Math.max(maxLat, lat)));
    }

    public void markPaused(long walkId) {
        redis.opsForHash().put(key(walkId), "pausedAt", OffsetDateTime.now().toString());
    }

    public void markResumed(long walkId) {
        String k = key(walkId);
        Map<Object, Object> h = redis.opsForHash().entries(k);
        String pausedAtStr = (String) h.get("pausedAt");
        if (pausedAtStr != null) {
            OffsetDateTime pausedAt = OffsetDateTime.parse(pausedAtStr);
            long elapsed = java.time.Duration.between(pausedAt, OffsetDateTime.now()).getSeconds();
            long accumulated = Long.parseLong((String) h.getOrDefault("totalPausedSeconds", "0"));
            redis.opsForHash().put(k, "totalPausedSeconds", String.valueOf(accumulated + elapsed));
            redis.opsForHash().delete(k, "pausedAt");
        }
    }

    public void evict(long walkId) { redis.delete(key(walkId)); }

    private double parseDoubleOr(Map<Object, Object> h, String key, double defaultVal) {
        Object v = h.get(key);
        return v == null ? defaultVal : Double.parseDouble((String) v);
    }
}
