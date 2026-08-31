package com.ne7k.safepaw.walk.repository.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ne7k.safepaw.walk.config.WalkProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class WalkPointRedisBuffer {

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final WalkProperties walkProperties;

    private String key(long walkId) { return "walk:points:" + walkId; }

    private Duration ttl() {
        return Duration.ofHours(walkProperties.session().bufferTtlHours());
    }

    public void rpushAll(long walkId, List<RedisWalkPoint> points) {
        if (points.isEmpty()) return;
        String[] serialized = points.stream().map(this::toJson).toArray(String[]::new);
        redis.opsForList().rightPushAll(key(walkId), serialized);
        redis.expire(key(walkId), ttl());
    }

    /** 읽기 전용 — Redis 데이터 유지. finish 시 DB 커밋 성공 전까지 데이터 보존. */
    public List<RedisWalkPoint> peek(long walkId) {
        String k = key(walkId);
        List<String> raw = redis.opsForList().range(k, 0, -1);
        return raw == null ? List.of() : raw.stream().map(this::fromJson).toList();
    }

    /** abort / evict 전용 */
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
