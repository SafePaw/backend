package com.ne7k.safepaw.territory.service;

import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.territory.domain.Territory;
import com.ne7k.safepaw.territory.domain.TerritoryStatus;
import com.ne7k.safepaw.territory.repository.TerritoryRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 동일 강아지 ACTIVE가 P_new와 겹치면 ST_Union으로 합집합 저장.
 * 안 겹치는 영토는 그대로 둠 (ACTIVE 복수 허용).
 */
@Component
@RequiredArgsConstructor
public class TerritoryMergeService {

    private final TerritoryRepository territoryRepository;
    private final TerritoryBuilder territoryBuilder;

    @Transactional
    public void mergeOverlappingSameDog(Territory newlyClaimed, String newWkt) {
        List<Long> overlapIds = territoryRepository.findSameDogOverlapIds(
                newlyClaimed.getDog().getId(), newlyClaimed.getId(), newWkt);
        if (overlapIds == null || overlapIds.isEmpty()) {
            return;
        }

        List<Long> allIds = new ArrayList<>(overlapIds);
        allIds.add(newlyClaimed.getId());

        String unionWkt = territoryRepository.unionPolygonsWkt(allIds);
        if (unionWkt == null || unionWkt.isBlank() || !unionWkt.trim().startsWith("POLYGON")) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "동일견 영토 병합 실패");
        }

        double area = territoryRepository.areaSquareMeters(unionWkt);
        Polygon unionPoly = territoryBuilder.parse(unionWkt);
        newlyClaimed.replaceGeom(unionPoly, area);

        for (Long oldId : overlapIds) {
            Territory old = territoryRepository.findById(oldId).orElse(null);
            if (old != null && old.getStatus() == TerritoryStatus.ACTIVE) {
                old.markConquered();
            }
        }
    }
}
