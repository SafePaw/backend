package com.ne7k.safepaw.walk.service;

import com.ne7k.safepaw.dog.domain.Dog;
import com.ne7k.safepaw.dog.repository.DogRepository;
import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.walk.domain.WalkSession;
import com.ne7k.safepaw.walk.domain.WalkStatus;
import com.ne7k.safepaw.walk.dto.response.WalkLiveResponse;
import com.ne7k.safepaw.walk.repository.WalkSessionRepository;
import com.ne7k.safepaw.walk.repository.redis.WalkSessionStateCache;
import com.ne7k.safepaw.walk.repository.redis.WalkState;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class WalkLiveService {

    private final WalkSessionRepository walkSessionRepository;
    private final WalkSessionStateCache stateCache;
    private final DogRepository dogRepository;

    @Value("${safepaw.walk.calorie-coefficient:1.1}")
    private double calorieCoefficient;

    private static final double DEFAULT_WEIGHT_KG = 10.0;
    private static final double BBOX_SHAPE_FACTOR = 0.65; // concave hull 보정

    /** 진행 중·일시정지 산책 실시간 snapshot */
    @Transactional(readOnly = true)
    public WalkLiveResponse live(Long userId, Long walkId) {
        WalkSession session = walkSessionRepository.findByIdAndDog_Owner_Id(walkId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALK_NOT_FOUND));

        if (session.getStatus() == WalkStatus.COMPLETED
                || session.getStatus() == WalkStatus.ABORTED) {
            throw new BusinessException(ErrorCode.WALK_ALREADY_FINISHED);
        }

        WalkState state = stateCache.get(walkId);

        // 경과 시간 (일시정지 시간 제외)
        long nowEpoch = OffsetDateTime.now().toEpochSecond();
        long startEpoch = state.startedAt().toEpochSecond();
        long elapsed = nowEpoch - startEpoch - state.totalPausedSeconds();
        // 일시정지 중이면 pausedAt 이후 경과도 제외
        if (state.isPaused() && state.pausedAt() != null) {
            elapsed -= (nowEpoch - state.pausedAt().toEpochSecond());
        }
        int durationSeconds = (int) Math.max(0, elapsed);

        double totalMeters = state.totalMeters();
        double distanceKm = totalMeters / 1000.0;

        // 평균 속도
        double avgSpeed = durationSeconds > 0
                ? GeoUtils.speedKmh(totalMeters, durationSeconds) : 0.0;

        // 현재 속도: prev → last 구간
        double currentSpeed = 0.0;
        if (state.hasPrev() && state.hasLast()) {
            long dt = java.time.Duration.between(state.prevAt(), state.lastAt()).getSeconds();
            if (dt > 0) {
                double d = GeoUtils.haversineMeters(
                        state.prevLng(), state.prevLat(), state.lastLng(), state.lastLat());
                currentSpeed = GeoUtils.speedKmh(d, dt);
            }
        }
        // 일시정지 중이면 현재 속도 = 0
        if (state.isPaused()) currentSpeed = 0.0;

        // 영토 추정 (bbox)
        double territory = estimateTerritoryM2(state);

        // 칼로리
        double weightKg = DEFAULT_WEIGHT_KG;
        Dog dog = dogRepository.findById(state.dogId()).orElse(null);
        if (dog != null && dog.getWeightKg() != null) {
            weightKg = dog.getWeightKg().doubleValue();
        }
        double calories = weightKg * distanceKm * calorieCoefficient;

        return new WalkLiveResponse(
                walkId,
                session.getStatus().name(),
                totalMeters,
                durationSeconds,
                Math.round(currentSpeed * 10.0) / 10.0,
                Math.round(avgSpeed * 10.0) / 10.0,
                state.pointCount(),
                territory,
                Math.round(calories * 10.0) / 10.0,
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
