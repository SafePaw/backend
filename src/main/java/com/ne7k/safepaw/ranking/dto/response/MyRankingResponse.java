package com.ne7k.safepaw.ranking.dto.response;

public record MyRankingResponse(
        String season,
        Long dogId,
        String dogName,
        String markerImageUrl,
        String markerImageType,
        String markerImageValue,
        String territoryColor,
        CategoryRanks rankings
) {
    public record CategoryRanks(
            RankSlice xp,
            RankSlice territory,
            RankSlice distance,
            RankSlice duration
    ) {}

    public record RankSlice(
            Integer rank,
            double value,
            String unit,
            Double percentile
    ) {}
}