package com.ne7k.safepaw.ranking.dto.response;

import java.util.List;

public record RankingBoardResponse(
        String season,
        String category,
        int page,
        int size,
        long totalElements,
        List<RankingEntryResponse> content
) {
}
