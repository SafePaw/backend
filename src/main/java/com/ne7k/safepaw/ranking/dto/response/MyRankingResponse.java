package com.ne7k.safepaw.ranking.dto.response;

public record MyRankingResponse(
        String season,
        Long dogId,
        String dogName,
        CategoryRanks rankings
) {
    public record CategoryRanks(
            RankSlice xp,         // 메인 — 항상 포함
            RankSlice territory,
            RankSlice distance,
            RankSlice duration
    ) {}

    public record RankSlice(
            Integer rank,      // null = 미참여
            double value,
            String unit,
            Double percentile  // null = 미참여
    ) {}
}
