package com.ne7k.safepaw.walk.dto.response;

import com.ne7k.safepaw.walk.domain.WalkSession;
import java.time.OffsetDateTime;

public record WalkStartResponse(
        Long walkSessionId, Long dogId, String status, OffsetDateTime startedAt
) {
    public static WalkStartResponse from(WalkSession w) {
        return new WalkStartResponse(w.getId(), w.getDog().getId(),
                w.getStatus().name(), w.getStartedAt());
    }
}