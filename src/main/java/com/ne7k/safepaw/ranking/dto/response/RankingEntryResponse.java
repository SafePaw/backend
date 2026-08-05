package com.ne7k.safepaw.ranking.dto.response;

public record RankingEntryResponse(
        int rank,
        Long dogId,
        String dogName,
        String markerImageUrl,
        String markerImageType,
        String markerImageValue,
        String rankBadge,
        String territoryColor,
        double value,
        String unit
) {
}