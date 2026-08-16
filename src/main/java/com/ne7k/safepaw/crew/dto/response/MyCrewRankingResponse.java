package com.ne7k.safepaw.crew.dto.response;

public record MyCrewRankingResponse(
        String season,
        Long crewId,
        String crewName,
        String territoryColor,
        String imageUrl,
        Integer rank,
        double value,
        String unit,
        Double percentile
) {}
