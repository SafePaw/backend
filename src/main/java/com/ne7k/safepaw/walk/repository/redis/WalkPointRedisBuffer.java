package com.ne7k.safepaw.walk.repository.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class WalkPointRedisBuffer {

    private final StringRedisTemplate redis;
    // json 변환
    private final ObjectMapper mapper;
    // 키 만료
    private static final Duration TTL = Duration.ofHours(24);

    // 키 구조
    private String key(long walkId) { return "walk:points:" + walkId; }

    // 적재
    public void rpushAll(long walkId, List<RedisWalkPoint> points) {
        if (points.isEmpty()) return;
        String[] serialized = points.stream().map(this::toJson).toArray(String[]::new);
        redis.opsForList().rightPushAll(key(walkId), serialized);
        redis.expire(key(walkId), TTL);
    }

    // 종료 시 한 번에 가져오고 키 제거
    public List<RedisWalkPoint> drain(long walkId) {
        String k = key(walkId);
        List<String> raw = redis.opsForList().range(k, 0, -1);
        redis.delete(k);
        return raw == null ? List.of() : raw.stream().map(this::fromJson).toList();
    }

    public void evict(long walkId) { redis.delete(key(walkId)); }

    private String toJson(RedisWalkPoint p) {
        try { return mapper.writeValueAsString(p); }
        catch (Exception e) { throw new IllegalStateException("RedisWalkPoint 직렬화 실패", e); }
    }
    private RedisWalkPoint fromJson(String s) {
        try { return mapper.readValue(s, RedisWalkPoint.class); }
        catch (Exception e) { throw new IllegalStateException("RedisWalkPoint 역직렬화 실패", e); }
    }
}