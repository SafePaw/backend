package com.ne7k.safepaw.walk.dto.response;

import com.ne7k.safepaw.dog.domain.Dog;
import com.ne7k.safepaw.score.service.XpService;
import com.ne7k.safepaw.territory.domain.Territory;
import com.ne7k.safepaw.territory.dto.response.GeoJsonPolygon;
import com.ne7k.safepaw.territory.service.PartialConquestService;
import com.ne7k.safepaw.walk.domain.WalkSession;

import java.util.List;

public record WalkFinishResponse(
        Long walkSessionId,
        String status,
        String walkType,              // TERRITORY | NORMAL
        WalkStats stats,
        TerritoryPart territory,      // null 이면 일반 산책
        String ineligibleReason,      // 일반 산책일 때만
        String ineligibleMessage,
        List<IntrusionPart> intrusions,
        List<XpPart> xpGained,
        int totalXpAfter,
        String rankAfter,
        boolean rankUp
) {
    public record TerritoryPart(Long id, GeoJsonPolygon polygon, double areaSquareMeters) {}
    public record IntrusionPart(
            String victimDogName,
            double overlapRatio,
            Long victimTerritoryId,
            GeoJsonPolygon stolenPolygon,
            GeoJsonPolygon victimRemainderPolygon,  // null if victimStatusAfter=CONQUERED
            double victimRemainderAreaSquareMeters,
            String victimStatusAfter              // ACTIVE | CONQUERED
    ) {}
    public record XpPart(String source, int amount) {}

    private static List<XpPart> toXp(List<XpService.Grant> grants) {
        return grants.stream().map(g -> new XpPart(g.source().name(), g.amount())).toList();
    }

    public static WalkFinishResponse territory(
            WalkSession s, double distance, int duration, double avgSpeed, int pointCount,
            Double loopGap, Territory t, double area,
            List<PartialConquestService.Result> intr, List<XpService.Grant> grants, Dog dog) {
        return new WalkFinishResponse(
                s.getId(), s.getStatus().name(), "TERRITORY",
                new WalkStats(distance, duration, avgSpeed, pointCount, loopGap),
                new TerritoryPart(t.getId(), GeoJsonPolygon.from(t.getGeom()), area),
                null, null,
                intr.stream().map(i -> new IntrusionPart(
                        i.victimDogName(), i.overlapRatio(), i.victimTerritoryId(),
                        (GeoJsonPolygon) i.stolenPolygon(),
                        i.victimRemainderPolygon() != null ? (GeoJsonPolygon) i.victimRemainderPolygon() : null,
                        i.victimRemainderAreaSquareMeters(), i.victimStatusAfter())).toList(),
                toXp(grants), dog.getTotalXp(), dog.getRank().name(), false);
    }

    public static WalkFinishResponse normal(
            WalkSession s, double distance, int duration, double avgSpeed, int pointCount,
            Double loopGap, String reason, String message, List<XpService.Grant> grants, Dog dog) {
        return new WalkFinishResponse(
                s.getId(), s.getStatus().name(), "NORMAL",
                new WalkStats(distance, duration, avgSpeed, pointCount, loopGap),
                null, reason, message, List.of(),
                toXp(grants), dog.getTotalXp(), dog.getRank().name(), false);
    }
}
