package com.ne7k.safepaw.walk.service;

import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.walk.domain.WalkSession;
import com.ne7k.safepaw.walk.domain.WalkStatus;
import com.ne7k.safepaw.walk.dto.response.WalkLiveResponse;
import com.ne7k.safepaw.walk.repository.WalkSessionRepository;
import com.ne7k.safepaw.walk.repository.redis.WalkSessionStateCache;
import com.ne7k.safepaw.walk.repository.redis.WalkState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class WalkLiveService {

    private final WalkSessionRepository walkSessionRepository;
    private final WalkSessionStateCache stateCache;
    private final CalorieCalculator calorieCalculator;

    private static final double BBOX_SHAPE_FACTOR = 0.65;

    @Transactional(readOnly = true)
    public WalkLiveResponse live(Long userId, Long walkId) {
        WalkSession session = walkSessionRepository.findByIdAndDog_Owner_Id(walkId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALK_NOT_FOUND));

        if (session.getStatus() == WalkStatus.COMPLETED
                || session.getStatus() == WalkStatus.ABORTED) {
            throw new BusinessException(ErrorCode.WALK_ALREADY_FINISHED);
        }

        WalkState state = stateCache.get(walkId);

        long nowEpoch = OffsetDateTime.now().toEpochSecond();
        long startEpoch = state.startedAt().toEpochSecond();
        long elapsed = nowEpoch - startEpoch - state.totalPausedSeconds();
        if (state.isPaused() && state.pausedAt() != null) {
            elapsed -= (nowEpoch - state.pausedAt().toEpochSecond());
        }
        int durationSeconds = (int) Math.max(0, elapsed);

        double totalMeters = state.totalMeters();

        double avgSpeed = durationSeconds > 0
                ? GeoUtils.speedKmh(totalMeters, durationSeconds) : 0.0;

        double currentSpeed = 0.0;
        if (state.hasPrev() && state.hasLast()) {
            double dt = java.time.Duration.between(state.prevAt(), state.lastAt()).toMillis() / 1000.0;
            if (dt > 0) {
                double d = GeoUtils.haversineMeters(
                        state.prevLng(), state.prevLat(), state.lastLng(), state.lastLat());
                currentSpeed = GeoUtils.speedKmh(d, dt);
            }
        }
        if (state.isPaused()) currentSpeed = 0.0;

        double territory = estimateTerritoryM2(state);

        double calories = calorieCalculator.kcal(session.getDog().getWeightKg(), totalMeters);

        return new WalkLiveResponse(
                walkId,
                session.getStatus().name(),
                totalMeters,
                durationSeconds,
                Math.round(currentSpeed * 10.0) / 10.0,
                Math.round(avgSpeed * 10.0) / 10.0,
                state.pointCount(),
                territory,
                calories,
                state.pausedAt(),
                (int) state.totalPausedSeconds()
        );
    }

    private double estimateTerritoryM2(WalkState state) {
        if (state.minLng() == null || state.maxLng() == null) return 0.0;
        if (state.pointCount() < 3) return 0.0;

        double midLat = (state.minLat() + state.maxLat()) / 2.0;
        double midLng = (state.minLng() + state.maxLng()) / 2.0;
        double widthM = GeoUtils.haversineMeters(state.minLng(), midLat, state.maxLng(), midLat);
        double heightM = GeoUtils.haversineMeters(midLng, state.minLat(), midLng, state.maxLat());
        double bbox = widthM * heightM;
        return Math.round(bbox * BBOX_SHAPE_FACTOR);
    }
}