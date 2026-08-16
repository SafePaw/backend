package com.ne7k.safepaw.crew.dto.response;

public record CrewStatsResponse(
        String season,
        Long crewId,
        String name,
        String territoryColor,
        String imageUrl,
        int memberCount,
        int maxMembers,
        long territoryCount,
        double areaSquareMeters,
        String unit
) {}
