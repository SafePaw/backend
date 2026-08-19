package com.ne7k.safepaw.walk.service;

import com.ne7k.safepaw.dog.domain.Dog;
import com.ne7k.safepaw.dog.repository.DogRepository;
import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.territory.domain.Territory;
import com.ne7k.safepaw.territory.repository.TerritoryRepository;
import com.ne7k.safepaw.territory.service.TerritoryService;
import com.ne7k.safepaw.walk.domain.WalkSession;
import com.ne7k.safepaw.walk.domain.WalkStatus;
import com.ne7k.safepaw.walk.dto.request.WalkPointDto;
import com.ne7k.safepaw.walk.dto.response.*;
import com.ne7k.safepaw.walk.repository.WalkPointRepository;
import com.ne7k.safepaw.walk.repository.WalkSessionRepository;
import com.ne7k.safepaw.walk.repository.redis.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
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
    private final CalorieCalculator calorieCalculator;
    private final PlatformTransactionManager transactionManager;

    public WalkStartResponse start(Long userId, Long dogId) {
        Dog dog = dogRepository.findByIdAndOwner_Id(dogId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOG_NOT_FOUND));

        lockManager.acquire(userId, -1);
        WalkSession saved;
        try {
            saved = persistStart(dog);
        } catch (RuntimeException e) {
            lockManager.release(userId);
            throw e;
        }

        stateCache.init(saved.getId(), userId, dogId, saved.getStartedAt());
        return WalkStartResponse.from(saved);
    }

    private WalkSession persistStart(Dog dog) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            walkSessionRepository.findActiveByDogId(dog.getId()).ifPresent(w -> {
                throw new BusinessException(ErrorCode.WALK_ONGOING_EXISTS);
            });
            return walkSessionRepository.save(WalkSession.start(dog));
        });
    }

    @Transactional(readOnly = true)
    public ActiveWalkListResponse listActive(Long userId, Long dogId) {
        if (dogId != null) {
            dogRepository.findByIdAndOwner_Id(dogId, userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DOG_NOT_FOUND));
        }

        List<WalkSession> sessions = (dogId != null)
                ? walkSessionRepository.findActiveByOwnerIdAndDogId(userId, dogId)
                : walkSessionRepository.findActiveByOwnerId(userId);

        List<ActiveWalkResponse> walks = sessions.stream()
                .map(ActiveWalkResponse::from)
                .toList();
        return ActiveWalkListResponse.of(walks);
    }

    @Transactional(readOnly = true)
    public Page<WalkHistoryItemResponse> listHistory(
            Long userId,
            Long dogId,
            Boolean territoryOnly,
            List<WalkStatus> statuses,
            int page,
            int size) {

        if (dogId != null) {
            dogRepository.findByIdAndOwner_Id(dogId, userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DOG_NOT_FOUND));
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Order.desc("endedAt"), Sort.Order.desc("startedAt"))
        );

        Page<WalkSession> sessions = (dogId != null)
                ? walkSessionRepository.findHistoryByOwnerIdAndDogId(
                userId, dogId, statuses, territoryOnly, pageable)
                : walkSessionRepository.findHistoryByOwnerId(
                userId, statuses, territoryOnly, pageable);

        Map<Long, Territory> territoryByWalkId = loadTerritoriesForSessions(sessions.getContent());

        return sessions.map(w -> {
            double distance = w.getDistanceMeters() == null ? 0 : w.getDistanceMeters();
            Double calories = calorieCalculator.kcal(w.getDog().getWeightKg(), distance);
            return WalkHistoryItemResponse.from(w, territoryByWalkId.get(w.getId()), calories);
        });
    }

    private Map<Long, Territory> loadTerritoriesForSessions(List<WalkSession> sessions) {
        List<Long> walkIds = sessions.stream().map(WalkSession::getId).toList();
        if (walkIds.isEmpty()) {
            return Map.of();
        }
        return territoryRepository.findByWalkSession_IdIn(walkIds).stream()
                .collect(Collectors.toMap(
                        t -> t.getWalkSession().getId(),
                        Function.identity(),
                        (a, b) -> a
                ));
    }

    public WalkFinishResponse finish(Long userId, Long walkId, List<WalkPointDto> lastPoints) {
        WalkSession session = walkSessionRepository.findByIdAndDog_Owner_Id(walkId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALK_NOT_FOUND));
        if (!session.isActive()) {
            throw new BusinessException(ErrorCode.WALK_ALREADY_FINISHED);
        }

        List<RedisWalkPoint> buffered = new ArrayList<>(buffer.peek(walkId));
        if (lastPoints != null) {
            lastPoints.forEach(p -> buffered.add(new RedisWalkPoint(
                    p.lng(), p.lat(), p.accuracyMeters(), p.speedKmh(), p.recordedAt())));
        }
        buffered.sort(Comparator.comparing(RedisWalkPoint::recordedAt));

        WalkValidator.Result vr = validator.filter(null, null, null, buffered);
        List<RedisWalkPoint> valid = vr.accepted();

        int gpsSpan = valid.isEmpty() ? 0
                : (int) java.time.Duration.between(valid.get(0).recordedAt(),
                valid.get(valid.size() - 1).recordedAt()).getSeconds();
        int duration = Math.max(0, gpsSpan - pausedSeconds(walkId));
        double distance = vr.addedMeters();

        WalkFinishResponse response = territoryService.finishAndClaim(
                session, valid, distance, duration);

        safeEvict(walkId, userId);
        return response;
    }

    private int pausedSeconds(Long walkId) {
        try {
            WalkState state = stateCache.get(walkId);
            long total = state.totalPausedSeconds();
            if (state.isPaused() && state.pausedAt() != null) {
                total += java.time.Duration.between(state.pausedAt(), OffsetDateTime.now()).getSeconds();
            }
            return (int) Math.max(0, total);
        } catch (Exception e) {
            return 0;
        }
    }

    private void safeEvict(Long walkId, Long userId) {
        try {
            buffer.evict(walkId);
        } catch (Exception e) {
            log.warn("walk buffer evict failed walkId={}", walkId, e);
        }
        try {
            stateCache.evict(walkId);
        } catch (Exception e) {
            log.warn("walk state evict failed walkId={}", walkId, e);
        }
        try {
            lockManager.release(userId);
        } catch (Exception e) {
            log.warn("walk lock release failed userId={}", userId, e);
        }
    }

    @Transactional
    public void abort(Long userId, Long walkId) {
        WalkSession session = walkSessionRepository.findByIdAndDog_Owner_Id(walkId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALK_NOT_FOUND));
        session.abort();
        buffer.evict(walkId);
        stateCache.evict(walkId);
        lockManager.release(userId);
    }

    @Transactional(readOnly = true)
    public WalkDetailResponse detail(Long userId, Long walkId) {
        WalkSession s = walkSessionRepository.findByIdAndDog_Owner_Id(walkId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALK_NOT_FOUND));
        List<Object[]> lngLat = walkPointRepository.findLngLatByWalkSessionId(walkId);
        Long territoryId = territoryRepository.findByWalkSession_Id(walkId)
                .map(Territory::getId)
                .orElse(null);
        double distance = s.getDistanceMeters() == null ? 0 : s.getDistanceMeters();
        Double calories = calorieCalculator.kcal(s.getDog().getWeightKg(), distance);
        return WalkDetailResponse.from(s, lngLat, territoryId, calories);
    }
}