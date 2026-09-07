package com.ne7k.safepaw.walk.dto.response;

import com.ne7k.safepaw.territory.dto.response.GeoJsonGeometry;
import com.ne7k.safepaw.territory.dto.response.GeoJsonLineString;

import java.time.OffsetDateTime;

public record WalkSummaryResponse(
        Long walkId,
        String status,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        WalkStats stats,
        GeoJsonLineString polyline,
        TerritoryPart territory,
        OwnerPart owner
) {
    public record TerritoryPart(
            Long id,
            GeoJsonGeometry polygon,
            double areaSquareMeters,
            OffsetDateTime claimedAt
    ) {}

    public record OwnerPart(
            Long userId,
            String nickname,
            DogPart dog
    ) {}

    public record DogPart(
            Long id,
            String name,
            String markerImageUrl,
            String markerImageType,
            String markerImageValue,
            String territoryColor
    ) {}
}
