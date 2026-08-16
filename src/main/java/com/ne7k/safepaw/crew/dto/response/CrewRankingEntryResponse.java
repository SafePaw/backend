package com.ne7k.safepaw.crew.dto.response;

public record CrewRankingEntryResponse(
        int rank,
        Long crewId,
        String crewName,
        String imageUrl,
        String territoryColor,
        int memberCount,
        double value,
        String unit
) {}
