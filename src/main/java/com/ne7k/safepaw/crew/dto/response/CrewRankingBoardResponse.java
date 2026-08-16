package com.ne7k.safepaw.crew.dto.response;

import java.util.List;

public record CrewRankingBoardResponse(
        String season,
        String category,
        int page,
        int size,
        long totalElements,
        List<CrewRankingEntryResponse> content
) {}
