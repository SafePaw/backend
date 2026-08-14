package com.ne7k.safepaw.walk.dto.response;

import com.ne7k.safepaw.territory.domain.Territory;

public record WalkHistoryTerritorySummary(
        Long id,
        double areaSquareMeters,
        String status
) {
    public static WalkHistoryTerritorySummary from(Territory t) {
        return new WalkHistoryTerritorySummary(
                t.getId(),
                t.getAreaSquareMeters(),
                t.getStatus().name()
        );
    }
}