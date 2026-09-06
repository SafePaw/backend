package com.ne7k.safepaw.walk.service;

import com.ne7k.safepaw.dog.service.MarkerUrlResolver;
import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.territory.domain.Territory;
import com.ne7k.safepaw.territory.dto.response.GeoJsonGeometry;
import com.ne7k.safepaw.territory.dto.response.GeoJsonLineString;
import com.ne7k.safepaw.territory.repository.TerritoryRepository;
import com.ne7k.safepaw.walk.domain.WalkSession;
import com.ne7k.safepaw.walk.dto.response.WalkStats;
import com.ne7k.safepaw.walk.dto.response.WalkSummaryResponse;
import com.ne7k.safepaw.walk.repository.WalkPointRepository;
import com.ne7k.safepaw.walk.repository.WalkSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WalkSummaryService {

    private final WalkSessionRepository walkSessionRepository;
    private final WalkPointRepository walkPointRepository;
    private final TerritoryRepository territoryRepository;
    private final CalorieCalculator calorieCalculator;
    private final MarkerUrlResolver markerUrlResolver;

    @Transactional(readOnly = true)
    public WalkSummaryResponse summary(Long userId, Long walkId) {
        WalkSession session = walkSessionRepository.findByIdAndDog_Owner_Id(walkId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALK_NOT_FOUND));

        // 통계
        double distance = session.getDistanceMeters() == null ? 0 : session.getDistanceMeters();
        int duration = session.getDurationSeconds() == null ? 0 : session.getDurationSeconds();
        Double calories = calorieCalculator.kcal(session.getDog().getWeightKg(), distance);
        WalkStats stats = WalkStats.of(distance, duration, session.getPointCount(), null, calories);

        // GPS 경로
        List<Object[]> lngLat = walkPointRepository.findLngLatByWalkSessionId(walkId);
        GeoJsonLineString polyline = lngLat.isEmpty() ? null : GeoJsonLineString.from(lngLat);

        // 영토 (null 가능)
        WalkSummaryResponse.TerritoryPart territoryPart = territoryRepository
                .findByWalkSession_Id(walkId)
                .map(t -> new WalkSummaryResponse.TerritoryPart(
                        t.getId(),
                        GeoJsonGeometry.from(t.getGeom()),
                        t.getAreaSquareMeters(),
                        t.getClaimedAt()))
                .orElse(null);

        // 사용자(닉네임) + 강아지(마커)
        var dog = session.getDog();
        var marker = markerUrlResolver.resolveFields(dog.getMarkerImageKey());
        var dogPart = new WalkSummaryResponse.DogPart(
                dog.getId(),
                dog.getName(),
                marker.url(),
                marker.type() != null ? marker.type().name() : null,
                marker.value(),
                dog.getTerritoryColor()
        );
        var ownerPart = new WalkSummaryResponse.OwnerPart(
                dog.getOwner().getId(),
                dog.getOwner().getNickname(),
                dogPart
        );

        return new WalkSummaryResponse(
                session.getId(),
                session.getStatus().name(),
                session.getStartedAt(),
                session.getEndedAt(),
                stats,
                polyline,
                territoryPart,
                ownerPart
        );
    }
}
