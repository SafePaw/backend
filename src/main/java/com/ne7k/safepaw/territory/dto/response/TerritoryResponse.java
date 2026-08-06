package com.ne7k.safepaw.territory.dto.response;

import com.ne7k.safepaw.dog.service.MarkerUrlResolver;
import com.ne7k.safepaw.territory.domain.Territory;

import java.time.OffsetDateTime;

public record TerritoryResponse(
        Long id,
        DogPart dog,
        GeoJsonPolygon polygon,
        double areaSquareMeters,
        boolean isMine,
        String status,
        OffsetDateTime claimedAt,
        OffsetDateTime conqueredAt
) {
    public record DogPart(
            Long id,
            String name,
            String rank,
            String territoryColor,
            String markerImageUrl,
            String markerImageType,
            String markerImageValue
    ) {}

    public static TerritoryResponse from(
            Territory t,
            Long viewerUserId,
            MarkerUrlResolver.MarkerFields marker
    ) {
        var d = t.getDog();
        return new TerritoryResponse(
                t.getId(),
                new DogPart(
                        d.getId(),
                        d.getName(),
                        d.getRank().name(),
                        d.getTerritoryColor(),
                        marker.url(),
                        marker.type() != null ? marker.type().name() : null,
                        marker.value()
                ),
                GeoJsonPolygon.from(t.getGeom()),
                t.getAreaSquareMeters(),
                d.isOwnedBy(viewerUserId),
                t.getStatus().name(),
                t.getClaimedAt(),
                t.getConqueredAt()
        );
    }
}