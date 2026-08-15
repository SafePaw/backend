package com.ne7k.safepaw.crew.dto.response;

import com.ne7k.safepaw.crew.domain.Crew;
import com.ne7k.safepaw.dog.domain.Dog;
import com.ne7k.safepaw.dog.service.MarkerUrlResolver;
import com.ne7k.safepaw.territory.domain.Territory;
import com.ne7k.safepaw.territory.dto.response.GeoJsonGeometry;
import com.ne7k.safepaw.territory.dto.response.TerritoryResponse;

import java.time.OffsetDateTime;

public record CrewTerritoryResponse(
        Long id,
        GeoJsonGeometry polygon,
        double areaSquareMeters,
        String status,
        OffsetDateTime claimedAt,
        OffsetDateTime conqueredAt,
        boolean isMine,
        String fillColor,
        TerritoryResponse.DogPart dog,
        TerritoryResponse.CrewPart crew
) {
    public static CrewTerritoryResponse from(
            Territory t,
            Long viewerUserId,
            MarkerUrlResolver.MarkerFields marker,
            Crew crew,
            String crewImageUrl
    ) {
        Dog d = t.getDog();
        return new CrewTerritoryResponse(
                t.getId(),
                GeoJsonGeometry.from(t.getGeom()),
                t.getAreaSquareMeters(),
                t.getStatus().name(),
                t.getClaimedAt(),
                t.getConqueredAt(),
                d.isOwnedBy(viewerUserId),
                crew.getTerritoryColor(),
                new TerritoryResponse.DogPart(
                        d.getId(),
                        d.getName(),
                        d.getRank().name(),
                        d.getTerritoryColor(),
                        marker.url(),
                        marker.type() != null ? marker.type().name() : null,
                        marker.value()
                ),
                new TerritoryResponse.CrewPart(
                        crew.getId(),
                        crew.getName(),
                        crew.getTerritoryColor(),
                        crewImageUrl
                )
        );
    }
}
