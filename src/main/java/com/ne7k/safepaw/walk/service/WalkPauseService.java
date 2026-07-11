package com.ne7k.safepaw.walk.service;

import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.walk.domain.WalkSession;
import com.ne7k.safepaw.walk.dto.response.WalkPauseResponse;
import com.ne7k.safepaw.walk.dto.response.WalkResumeResponse;
import com.ne7k.safepaw.walk.repository.WalkSessionRepository;
import com.ne7k.safepaw.walk.repository.redis.WalkSessionStateCache;
import com.ne7k.safepaw.walk.repository.redis.WalkState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class WalkPauseService {

    private final WalkSessionRepository walkSessionRepository;
    private final WalkSessionStateCache stateCache;

    @Transactional
    public WalkPauseResponse pause(Long userId, Long walkId) {
        WalkSession session = walkSessionRepository.findByIdAndDog_Owner_Id(walkId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALK_NOT_FOUND));

        if (session.isPaused()) {
            throw new BusinessException(ErrorCode.WALK_ALREADY_PAUSED);
        }
        session.pause(); // DB status → PAUSED

        OffsetDateTime pausedAt = OffsetDateTime.now();
        stateCache.markPaused(walkId); // Redis pausedAt 기록

        return new WalkPauseResponse(walkId, "PAUSED", pausedAt);
    }

    @Transactional
    public WalkResumeResponse resume(Long userId, Long walkId) {
        WalkSession session = walkSessionRepository.findByIdAndDog_Owner_Id(walkId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALK_NOT_FOUND));

        if (!session.isPaused()) {
            throw new BusinessException(ErrorCode.WALK_NOT_PAUSED);
        }

        stateCache.markResumed(walkId); // Redis totalPausedSeconds 누적 + pausedAt 삭제
        WalkState state = stateCache.get(walkId);

        session.resume(); // DB status → ONGOING

        return new WalkResumeResponse(walkId, "ONGOING", (int) state.totalPausedSeconds());
    }
}