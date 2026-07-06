package com.ne7k.safepaw.walk.service;

import com.ne7k.safepaw.dog.domain.Dog;
import com.ne7k.safepaw.dog.repository.DogRepository;
import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.walk.domain.*;
import com.ne7k.safepaw.walk.dto.request.WalkPointDto;
import com.ne7k.safepaw.walk.dto.response.*;
import com.ne7k.safepaw.walk.repository.WalkPointRepository;
import com.ne7k.safepaw.walk.repository.WalkSessionRepository;
import com.ne7k.safepaw.walk.repository.redis.*;
import com.ne7k.safepaw.territory.service.TerritoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

import com.ne7k.safepaw.territory.domain.Territory;
import com.ne7k.safepaw.territory.repository.TerritoryRepository;

@Service
@RequiredArgsConstructor
public class WalkSessionService {

    private final TerritoryRepository territoryRepository;
    private final DogRepository dogRepository;
    private final WalkSessionRepository walkSessionRepository;
    private final WalkPointRepository walkPointRepository;
    private final WalkPointRedisBuffer buffer;
    private final WalkSessionStateCache stateCache;
    private final WalkLockManager lockManager;
    private final WalkValidator validator;
    private final TerritoryService territoryService;

    // ---------- 시작 ----------
    public WalkStartResponse start(Long userId, Long dogId) {
        Dog dog = dogRepository.findByIdAndOwner_Id(dogId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOG_NOT_FOUND));

        // 1) 단일 산책 락 (Redis) — DB INSERT 전
        WalkSession saved;
        // 임시 walkId 없이 락을 먼저 잡기 위해 placeholder 후 보정: 여기선 DB 먼저 만들지 않으므로
        // 락 값은 userId 기준이며 walkId 는 INSERT 후 갱신 불필요(키가 user 단위라 존재 자체가 락).
        lockManager.acquire(userId, -1);
        try {
            // 2) DB INSERT (ONGOING) — 트랜잭션
            saved = persistStart(dog);
        } catch (RuntimeException e) {
            lockManager.release(userId); // 실패 시 락 해제
            throw e;
        }

        // 3) Redis 상태 캐시 init (트랜잭션 밖)
        stateCache.init(saved.getId(), userId, dogId, saved.getStartedAt());
        return WalkStartResponse.from(saved);
    }

    @Transactional
    protected WalkSession persistStart(Dog dog) {
        // 동일 강아지 ONGOING 중복 방지(락과 이중 안전망)
        walkSessionRepository.findOngoingByDogId(dog.getId()).ifPresent(w -> {
            throw new BusinessException(ErrorCode.WALK_ONGOING_EXISTS);
        });
        return walkSessionRepository.save(WalkSession.start(dog));
    }

    // ---------- 종료 ----------
    public WalkFinishResponse finish(Long userId, Long walkId, List<WalkPointDto> lastPoints) {
        WalkSession session = walkSessionRepository.findByIdAndDog_Owner_Id(walkId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALK_NOT_FOUND));
        if (!session.isOngoing()) {
            throw new BusinessException(ErrorCode.WALK_ALREADY_FINISHED);
        }

        // 1) Redis drain + lastPoints 병합 → 시간순 정렬 (Redis, 트랜잭션 밖)
        List<RedisWalkPoint> all = new java.util.ArrayList<>(buffer.drain(walkId));
        if (lastPoints != null) {
            lastPoints.forEach(p -> all.add(new RedisWalkPoint(
                    p.lng(), p.lat(), p.accuracyMeters(), p.speedKmh(), p.recordedAt())));
        }
        all.sort(Comparator.comparing(RedisWalkPoint::recordedAt));

        // 2) 전체 재검증 (정확도/속도/점프)
        WalkValidator.Result vr = validator.filter(null, null, null, all);
        List<RedisWalkPoint> valid = vr.accepted();

        // 3) 통계
        int duration = valid.isEmpty() ? 0
                : (int) java.time.Duration.between(valid.get(0).recordedAt(),
                valid.get(valid.size() - 1).recordedAt()).getSeconds();
        double distance = vr.addedMeters();
        double avgSpeed = GeoUtils.speedKmh(distance, duration);

        // 4) DB 트랜잭션: 포인트 bulk INSERT + 완료 + 영토 파이프라인
        WalkFinishResponse response = territoryService.finishAndClaim(
                session, valid, distance, duration, avgSpeed);

        // 5) Redis 정리 (트랜잭션 밖)
        stateCache.evict(walkId);
        lockManager.release(userId);
        buffer.evict(walkId);
        return response;
    }

    // ---------- 중도 포기 ----------
    @Transactional
    public void abort(Long userId, Long walkId) {
        WalkSession session = walkSessionRepository.findByIdAndDog_Owner_Id(walkId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALK_NOT_FOUND));
        session.abort();
        buffer.evict(walkId);
        stateCache.evict(walkId);
        lockManager.release(userId);
    }

    // ---------- 상세 ----------
    @Transactional(readOnly = true)
    public WalkDetailResponse detail(Long userId, Long walkId) {
        WalkSession s = walkSessionRepository.findByIdAndDog_Owner_Id(walkId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALK_NOT_FOUND));
        // 경로 polyline: walk_points → [lng, lat][] (§8.2 WalkDetailResponse, §8.3 GeoJsonLineString)
        List<Object[]> lngLat = walkPointRepository.findLngLatByWalkSessionId(walkId);
        Long territoryId = territoryRepository.findByWalkSession_Id(walkId)
                .map(Territory::getId)
                .orElse(null);
        return WalkDetailResponse.from(s, lngLat, territoryId);
    }
}