package com.ne7k.safepaw.walk.repository.redis;

import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class WalkLockManager {

    private final StringRedisTemplate redis;
    private static final Duration LOCK_TTL = Duration.ofHours(2);

    private String key(long userId) { return "walk:lock:user:" + userId; }

    /** 락 획득 실패 = 이미 진행 중인 산책 존재 */
    public void acquire(long userId, long walkId) {
        Boolean ok = redis.opsForValue().setIfAbsent(key(userId), String.valueOf(walkId), LOCK_TTL);
        if (!Boolean.TRUE.equals(ok)) {
            throw new BusinessException(ErrorCode.WALK_ONGOING_EXISTS);
        }
    }

    public void release(long userId) { redis.delete(key(userId)); }
}