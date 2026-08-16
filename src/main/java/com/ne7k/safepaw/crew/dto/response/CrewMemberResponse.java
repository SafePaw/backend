package com.ne7k.safepaw.crew.dto.response;

import java.time.OffsetDateTime;

public record CrewMemberResponse(
        Long userId,
        String nickname,
        String role,
        OffsetDateTime joinedAt,
        long dogCount,
        double activeAreaSquareMeters
) {}
