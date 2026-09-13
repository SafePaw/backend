package com.ne7k.safepaw.walk.dto.response;

import com.ne7k.safepaw.territory.domain.Territory;
import com.ne7k.safepaw.territory.service.TerritoryMarker;

public record WalkHistoryTerritorySummary(
        Long id,
        double areaSquareMeters,
        String status,
        Double markerLng,
        Double markerLat
) {
    public static WalkHistoryTerritorySummary from(Territory t) {
        var markerPt = TerritoryMarker.ofActive(t);
        return new WalkHistoryTerritorySummary(
                t.getId(),
                t.getAreaSquareMeters(),
                t.getStatus().name(),
                markerPt == null ? null : markerPt.lng(),
                markerPt == null ? null : markerPt.lat()
        );
    }
}