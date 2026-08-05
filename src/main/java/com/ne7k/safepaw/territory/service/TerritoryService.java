package com.ne7k.safepaw.territory.service;

import com.ne7k.safepaw.dog.domain.Dog;
import com.ne7k.safepaw.score.domain.Season;
import com.ne7k.safepaw.score.service.SeasonService;
import com.ne7k.safepaw.score.service.XpService;
import com.ne7k.safepaw.territory.domain.Territory;
import com.ne7k.safepaw.territory.repository.TerritoryRepository;
import com.ne7k.safepaw.walk.domain.*;
import com.ne7k.safepaw.walk.dto.response.WalkFinishResponse;
import com.ne7k.safepaw.walk.repository.WalkPointRepository;
import com.ne7k.safepaw.walk.repository.redis.RedisWalkPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ne7k.safepaw.territory.dto.response.TerritoryResponse;

import java.util.List;
import com.ne7k.safepaw.territory.domain.TerritoryStatus;
import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;

import com.ne7k.safepaw.dog.repository.DogRepository;
import com.ne7k.safepaw.walk.repository.WalkSessionRepository;

@Service
@RequiredArgsConstructor
public class TerritoryService {

    private final DogRepository dogRepository;
    private final WalkSessionRepository walkSessionRepository;
    private final WalkPointRepository walkPointRepository;
    private final TerritoryRepository territoryRepository;
    private final TerritoryEligibility eligibility;
    private final PartialConquestService partialConquest;
    private final SeasonService seasonService;
    private final XpService xpService;

    /**
     * 한 트랜잭션: 포인트 bulk INSERT → 세션 완료 → 영토 자격 판정 →
     *   통과: territory INSERT + 부분 점령(§6.6) + XP(완료+점령+보너스)
     *   실패: XP(완료만), ineligibleReason
     */
    @Transactional
    public WalkFinishResponse finishAndClaim(WalkSession session, List<RedisWalkPoint> valid,
                                             double distance, int duration, double avgSpeed) {
        Long walkId = session.getId();
        WalkSession managedSession = walkSessionRepository.findById(walkId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALK_NOT_FOUND));
        Dog dog = dogRepository.findById(managedSession.getDog().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DOG_NOT_FOUND));

        // 1) 포인트 bulk INSERT
        List<WalkPoint> entities = valid.stream()
                .map(p -> WalkPoint.of(managedSession, p.lng(), p.lat(), p.accuracyMeters(), p.speedKmh(), p.recordedAt()))
                .toList();
        walkPointRepository.saveAll(entities);

        // 2) 세션 완료 (거리·시간 항상 기록)
        managedSession.complete(distance, duration, valid.size());

        Season season = seasonService.currentSeason();

        // 3) 영토 자격 판정
        TerritoryEligibility.Outcome outcome =
                eligibility.evaluate(dog.getId(), walkId, duration, valid.size());

        if (!outcome.eligible()) {
            // 일반 산책: WALK_COMPLETED XP 만
            var grants = xpService.award(dog, season, managedSession, null, false, false);
            return WalkFinishResponse.normal(managedSession, distance, duration, avgSpeed,
                    valid.size(), outcome.loopGapMeters(),
                    outcome.reason().name(), outcome.message(), grants, dog);
        }

        // 4) 영토 INSERT
        boolean firstClaim = !territoryRepository.existsByDog_IdAndStatus(
                dog.getId(), TerritoryStatus.ACTIVE);

        Territory territory = territoryRepository.save(
                Territory.claim(dog, managedSession, season, outcome.polygon(), outcome.areaSquareMeters()));

        // 5) 부분 점령 (겹친 조각만 침범자 소유, 피해자 geom UPDATE)
        List<PartialConquestService.Result> intrusions;
        try {
            intrusions = partialConquest.apply(territory, dog.getId(), outcome.wkt());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "부분 점령 처리 실패");
        }

        // 6) XP (완료 + 점령 + 첫 점령 보너스)
        var grants = xpService.award(dog, season, managedSession, territory, true, firstClaim);

        return WalkFinishResponse.territory(managedSession, distance, duration, avgSpeed,
                valid.size(), outcome.loopGapMeters(), territory, outcome.areaSquareMeters(),
                intrusions, grants, dog);
    }

    @Transactional(readOnly = true)
    public Territory findById(Long territoryId) {
        return territoryRepository.findById(territoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TERRITORY_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<TerritoryResponse> findInBbox(
            double swLng, double swLat, double neLng, double neLat, Long viewerUserId) {
        return territoryRepository.findActiveInBbox(swLng, swLat, neLng, neLat).stream()
                .map(t -> TerritoryResponse.from(t, viewerUserId))
                .toList();
    }
}