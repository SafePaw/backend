package com.ne7k.safepaw.walk.service;

import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.walk.config.WalkProperties;
import com.ne7k.safepaw.walk.dto.request.WalkPointDto;
import com.ne7k.safepaw.walk.repository.redis.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WalkPointAppendService {

    private final WalkSessionStateCache stateCache;
    private final WalkPointRedisBuffer buffer;
    private final WalkValidator validator;
    private final WalkProperties props;

    /** DB 안 거치고 Redis 만. 응답 < 50ms. */
    public void append(Long userId, Long walkId, List<WalkPointDto> points) {
        if (points == null || points.isEmpty()
                || points.size() > props.batch().maxPoints()) {
            throw new BusinessException(ErrorCode.WALK_INVALID_POINT_BATCH);
        }

        // 진행 중 상태 + 소유권 확인 (캐시 없으면 WALK_NOT_ONGOING)
        WalkState state = stateCache.get(walkId);
        if (state.userId() != userId) {
            throw new BusinessException(ErrorCode.WALK_NOT_ONGOING);
        }

        if (state.isPaused()) {
            throw new BusinessException(ErrorCode.WALK_NOT_ONGOING);
        }

        List<RedisWalkPoint> batch = points.stream()
                .map(p -> new RedisWalkPoint(p.lng(), p.lat(), p.accuracyMeters(), p.speedKmh(), p.recordedAt()))
                .toList();

        // 직전 포인트(state)로 점프/속도 재검증
        WalkValidator.Result vr = validator.filter(
                state.lastLng(), state.lastLat(), state.lastAt(), batch);

        if (!vr.accepted().isEmpty()) {
            buffer.rpushAll(walkId, vr.accepted());
            RedisWalkPoint last = vr.accepted().get(vr.accepted().size() - 1);
            stateCache.updateAfterBatch(walkId, last, vr.addedMeters(), vr.accepted().size());
        }
        // 전부 걸러져도 202 (클라가 재전송할 필요 없음)
    }
}