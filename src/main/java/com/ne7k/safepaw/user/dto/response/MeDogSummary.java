package com.ne7k.safepaw.user.dto.response;

import com.ne7k.safepaw.dog.domain.DogRank;
import com.ne7k.safepaw.dog.domain.Gender;

public record MeDogSummary(
        Long id,
        String name,
        Gender gender,
        DogRank rank,
        int totalXp,
        String territoryColor
) {
}
