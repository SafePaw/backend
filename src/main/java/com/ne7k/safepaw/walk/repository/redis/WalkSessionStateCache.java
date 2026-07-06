package com.ne7k.safepaw.walk.repository.redis;

import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
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
    private static final Duration TTL = Duration.ofHours(24);

    private String key(long walkId) { return "walk:state:" + walkId; }

    public void init(long walkId, long userId, long dogId, OffsetDateTime startedAt) {
        Map<String, String> h = new HashMap<>();
        h.put("userId", String.valueOf(userId));
        h.put("dogId", String.valueOf(dogId));
        h.put("startedAt", startedAt.toString());
        h.put("pointCount", "0");
        h.put("totalMeters", "0");
        redis.opsForHash().putAll(key(walkId), h);
        redis.expire(key(walkId), TTL);
    }

    public WalkState get(long walkId) {
        Map<Object, Object> h = redis.opsForHash().entries(key(walkId));
        if (h.isEmpty()) throw new BusinessException(ErrorCode.WALK_NOT_ONGOING);
        return WalkState.from(h);
    }

    /** 배치 검증 후: 마지막 포인트 + 누적 거리/개수 갱신 */
    public void updateAfterBatch(long walkId, RedisWalkPoint last, double addedMeters, int addedCount) {
        String k = key(walkId);
        redis.opsForHash().put(k, "lastLng", String.valueOf(last.lng()));
        redis.opsForHash().put(k, "lastLat", String.valueOf(last.lat()));
        redis.opsForHash().put(k, "lastAt", last.recordedAt().toString());
        redis.opsForHash().increment(k, "pointCount", addedCount);
        // increment 는 정수 전용 → 미터는 putAll 로 재기록
        double newTotal = Double.parseDouble(String.valueOf(redis.opsForHash().get(k, "totalMeters"))) + addedMeters;
        redis.opsForHash().put(k, "totalMeters", String.valueOf(newTotal));
    }

    public void evict(long walkId) { redis.delete(key(walkId)); }
}