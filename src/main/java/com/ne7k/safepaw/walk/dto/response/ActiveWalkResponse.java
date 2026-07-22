package com.ne7k.safepaw.walk.dto.response;

import com.ne7k.safepaw.walk.domain.WalkSession;

import java.time.OffsetDateTime;

public record ActiveWalkResponse(
        Long walkId,
        Long dogId,
        String dogName,
        String status,
        OffsetDateTime startedAt
) {

    public static ActiveWalkResponse from(WalkSession w) {
        return new ActiveWalkResponse(
                w.getId(),
                w.getDog().getId(),
                w.getDog().getName(),
                w.getStatus().name(),
                w.getStartedAt()
        );
    }
}
