package com.ne7k.safepaw.walk.service;

import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.walk.config.WalkProperties;
import com.ne7k.safepaw.walk.dto.request.WalkPointDto;
import com.ne7k.safepaw.walk.repository.WalkPointBatchInsert;
import com.ne7k.safepaw.walk.repository.redis.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalkPointAppendService {

    private final WalkSessionStateCache stateCache;
    private final WalkPointRedisBuffer buffer;
    private final WalkValidator validator;
    private final WalkProperties props;
    private final WalkPointBatchInsert batchInsert;

    /**
     * GPS 배치 업로드.
     * - Redis 버퍼에 저장 (실시간 live 통계용)
     * - DB에도 백업 저장 (Redis TTL 만료·재시작 시 데이터 유실 방지)
     */
    @Transactional
    public void append(Long userId, Long walkId, List<WalkPointDto> points) {
        if (points == null || points.isEmpty()
                || points.size() > props.batch().maxPoints()) {
            throw new BusinessException(ErrorCode.WALK_INVALID_POINT_BATCH);
        }

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

        WalkValidator.Result vr = validator.filter(
                state.lastLng(), state.lastLat(), state.lastAt(), batch);

        if (!vr.accepted().isEmpty()) {
            buffer.rpushAll(walkId, vr.accepted());
            RedisWalkPoint last = vr.accepted().get(vr.accepted().size() - 1);
            stateCache.updateAfterBatch(walkId, last, vr.addedMeters(), vr.accepted().size());

            // Redis 만료·재시작에도 GPS 데이터가 유지되도록 DB에 백업 저장
            // ON CONFLICT DO NOTHING 으로 중복 삽입 무시
            batchInsert.batchInsert(walkId, vr.accepted());
        }
    }
}
