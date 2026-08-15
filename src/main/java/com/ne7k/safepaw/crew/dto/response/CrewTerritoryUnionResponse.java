package com.ne7k.safepaw.crew.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

public record CrewTerritoryUnionResponse(
        String season,
        Long crewId,
        String territoryColor,
        double areaSquareMeters,
        JsonNode geometry
) {}
