package com.ne7k.safepaw.territory.service;

import com.ne7k.safepaw.crew.repository.CrewMemberRepository;
import com.ne7k.safepaw.dog.domain.Dog;
import com.ne7k.safepaw.dog.domain.DogRank;
import com.ne7k.safepaw.dog.repository.DogRepository;
import com.ne7k.safepaw.dog.service.MarkerUrlResolver;
import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.score.domain.Season;
import com.ne7k.safepaw.score.service.SeasonService;
import com.ne7k.safepaw.score.service.XpService;
import com.ne7k.safepaw.territory.domain.Territory;
import com.ne7k.safepaw.territory.domain.TerritoryStatus;
import com.ne7k.safepaw.territory.dto.response.TerritoryResponse;
import com.ne7k.safepaw.territory.repository.TerritoryRepository;
import com.ne7k.safepaw.walk.domain.WalkPoint;
import com.ne7k.safepaw.walk.domain.WalkSession;
import com.ne7k.safepaw.walk.dto.response.WalkFinishResponse;
import com.ne7k.safepaw.walk.repository.WalkPointRepository;
import com.ne7k.safepaw.walk.repository.WalkSessionRepository;
import com.ne7k.safepaw.walk.repository.redis.RedisWalkPoint;
import com.ne7k.safepaw.walk.service.CalorieCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TerritoryService {

    private final DogRepository dogRepository;
    private final WalkSessionRepository walkSessionRepository;
    private final WalkPointRepository walkPointRepository;
    private final TerritoryRepository territoryRepository;
    private final TerritoryEligibility eligibility;
    private final PartialConquestService partialConquest;
    private final TerritoryMergeService territoryMergeService;
    private final SeasonService seasonService;
    private final XpService xpService;
    private final MarkerUrlResolver markerUrlResolver;
    private final CrewMemberRepository crewMemberRepository;
    private final CalorieCalculator calorieCalculator;

    @Transactional
    public WalkFinishResponse finishAndClaim(WalkSession session, List<RedisWalkPoint> valid,
                                             double distance, int duration) {
        Long walkId = session.getId();
        WalkSession managedSession = walkSessionRepository.findById(walkId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALK_NOT_FOUND));
        Dog dog = dogRepository.findById(managedSession.getDog().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DOG_NOT_FOUND));

        List<WalkPoint> entities = valid.stream()
                .map(p -> WalkPoint.of(managedSession, p.lng(), p.lat(), p.accuracyMeters(), p.speedKmh(), p.recordedAt()))
                .toList();
        walkPointRepository.saveAll(entities);

        managedSession.complete(distance, duration, valid.size());

        Double calories = calorieCalculator.kcal(dog.getWeightKg(), distance);

        Season season = seasonService.currentSeason();

        TerritoryEligibility.Outcome outcome =
                eligibility.evaluate(dog.getId(), walkId, duration, valid.size());

        DogRank rankBefore = dog.getRank();

        if (!outcome.eligible()) {
            var grants = xpService.award(dog, season, managedSession, null, false, false);
            boolean rankUp = dog.getRank() != rankBefore;
            return WalkFinishResponse.normal(managedSession, distance, duration,
                    valid.size(), outcome.loopGapMeters(), calories,
                    outcome.reason().name(), outcome.message(), grants, dog, rankUp);
        }

        boolean firstClaim = !territoryRepository.existsByDog_IdAndStatus(
                dog.getId(), TerritoryStatus.ACTIVE);

        Territory territory = territoryRepository.save(
                Territory.claim(dog, managedSession, season, outcome.polygon(), outcome.areaSquareMeters()));

        List<PartialConquestService.Result> intrusions;
        try {
            Long ownerId = dog.getOwner().getId();
            intrusions = partialConquest.apply(territory, dog.getId(), ownerId, outcome.wkt());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "부분 점령 처리 실패");
        }

        try {
            territoryMergeService.mergeOverlappingSameDog(territory, outcome.wkt());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "동일견 영토 병합 실패");
        }

        double finalArea = territory.getAreaSquareMeters();

        var grants = xpService.award(dog, season, managedSession, territory, true, firstClaim);
        boolean rankUp = dog.getRank() != rankBefore;

        return WalkFinishResponse.territory(managedSession, distance, duration,
                valid.size(), outcome.loopGapMeters(), calories, territory, finalArea,
                intrusions, grants, dog, rankUp);
    }

    @Transactional(readOnly = true)
    public Territory findById(Long territoryId) {
        return territoryRepository.findById(territoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TERRITORY_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public TerritoryResponse getDetail(Long territoryId, Long viewerUserId) {
        Territory t = findById(territoryId);
        return toResponses(List.of(t), viewerUserId).get(0);
    }

    @Transactional(readOnly = true)
    public List<TerritoryResponse> findInBbox(
            double swLng, double swLat, double neLng, double neLat, Long viewerUserId) {
        return toResponses(
                territoryRepository.findActiveInBbox(swLng, swLat, neLng, neLat),
                viewerUserId);
    }

    public TerritoryResponse toResponse(Territory t, Long viewerUserId) {
        return toResponses(List.of(t), viewerUserId).get(0);
    }

    public List<TerritoryResponse> toResponses(List<Territory> list, Long viewerUserId) {
        Map<Long, TerritoryResponse.CrewPart> crewByOwner = loadCrewParts(list);
        return list.stream()
                .map(t -> {
                    var marker = markerUrlResolver.resolveFields(t.getDog().getMarkerImageKey());
                    Long ownerId = t.getDog().getOwner().getId();
                    return TerritoryResponse.from(t, viewerUserId, marker, crewByOwner.get(ownerId));
                })
                .toList();
    }

    private Map<Long, TerritoryResponse.CrewPart> loadCrewParts(List<Territory> list) {
        List<Long> ownerIds = list.stream()
                .map(t -> t.getDog().getOwner().getId())
                .distinct()
                .toList();
        if (ownerIds.isEmpty()) {
            return Map.of();
        }
        return crewMemberRepository.findAllByUser_IdIn(ownerIds).stream()
                .collect(Collectors.toMap(
                        m -> m.getUser().getId(),
                        m -> new TerritoryResponse.CrewPart(
                                m.getCrew().getId(),
                                m.getCrew().getName(),
                                m.getCrew().getTerritoryColor(),
                                markerUrlResolver.resolve(m.getCrew().getImageKey())
                        )
                ));
    }
}