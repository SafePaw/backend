package com.ne7k.safepaw.territory.service;

import com.ne7k.safepaw.territory.config.TerritoryProperties;
import com.ne7k.safepaw.territory.domain.Territory;
import com.ne7k.safepaw.territory.domain.TerritoryIntrusion;
import com.ne7k.safepaw.territory.domain.TerritoryStatus;
import com.ne7k.safepaw.territory.event.TerritoryIntrusionEvent;
import com.ne7k.safepaw.territory.repository.TerritoryIntrusionRepository;
import com.ne7k.safepaw.territory.repository.TerritoryRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PartialConquestService {

    private final TerritoryRepository territoryRepository;
    private final TerritoryIntrusionRepository intrusionRepository;
    private final TerritoryProperties territoryProps;
    private final TerritoryBuilder territoryBuilder;
    private final ApplicationEventPublisher publisher;
    private final WKTReader wktReader = new WKTReader();

    /**
     * 타 dog ACTIVE와 겹치면 기존 geom Difference.
     * FCM/intrusion row는 피해자 owner ≠ 침범자 owner 일 때만.
     */
    public List<Result> apply(Territory newTerritory, Long myDogId, Long myOwnerId, String intruderWkt)
            throws Exception {
        List<Result> results = new ArrayList<>();

        for (var row : territoryRepository.findIntrusionCandidates(myDogId, intruderWkt)) {
            Territory victim = territoryRepository.findById(row.getTerritoryId()).orElse(null);
            if (victim == null || victim.getStatus() != TerritoryStatus.ACTIVE) {
                continue;
            }

            String overlapWkt = territoryRepository.intersectionWkt(victim.getId(), intruderWkt);
            String remainWkt = territoryRepository.differenceWkt(victim.getId(), intruderWkt);
            if (overlapWkt == null || overlapWkt.isBlank()) {
                continue;
            }

            Geometry overlapGeom = wktReader.read(overlapWkt);
            overlapGeom.setSRID(4326);

            Long victimOwnerId = victim.getDog().getOwner().getId();
            boolean crossOwner = !victimOwnerId.equals(myOwnerId);

            TerritoryIntrusion savedIntrusion = null;
            if (crossOwner) {
                savedIntrusion = intrusionRepository.save(TerritoryIntrusion.record(
                        victim, newTerritory, row.getOverlapRatio(), overlapGeom));
            }

            Polygon remainder = null;
            double remainderArea = 0;
            String victimStatusAfter = "ACTIVE";

            if (remainWkt != null && !remainWkt.isBlank() && !remainWkt.equals("POLYGON EMPTY")) {
                Geometry g = wktReader.read(remainWkt);
                g.setSRID(4326);
                if (g instanceof Polygon p && !p.isEmpty()) {
                    remainderArea = territoryRepository.areaSquareMeters(remainWkt);
                    if (remainderArea >= territoryProps.minAreaSquareMeters()) {
                        victim.shrinkToRemainder(p, remainderArea);
                        remainder = p;
                    } else {
                        victim.markConquered();
                        victimStatusAfter = "CONQUERED";
                    }
                } else {
                    victim.markConquered();
                    victimStatusAfter = "CONQUERED";
                }
            } else {
                victim.markConquered();
                victimStatusAfter = "CONQUERED";
            }

            if (crossOwner && savedIntrusion != null) {
                double stolenArea = territoryRepository.areaSquareMeters(overlapWkt);
                publisher.publishEvent(new TerritoryIntrusionEvent(
                        victimOwnerId,
                        savedIntrusion.getId(),
                        victim.getId(),
                        victim.getDog().getId(),
                        victim.getDog().getName(),
                        newTerritory.getId(),
                        newTerritory.getDog().getId(),
                        newTerritory.getDog().getName(),
                        row.getOverlapRatio(),
                        stolenArea,
                        remainderArea,
                        victimStatusAfter
                ));
            }

            results.add(new Result(
                    victim.getId(), victim.getDog().getName(), row.getOverlapRatio(),
                    territoryBuilder.toGeoJsonPolygon(overlapGeom),
                    remainder != null ? territoryBuilder.toGeoJsonPolygon(remainder) : null,
                    remainderArea, victimStatusAfter));
        }
        return results;
    }

    public record Result(
            Long victimTerritoryId,
            String victimDogName,
            double overlapRatio,
            Object stolenPolygon,
            Object victimRemainderPolygon,
            double victimRemainderAreaSquareMeters,
            String victimStatusAfter
    ) {}
}