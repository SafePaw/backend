package com.ne7k.safepaw.walk.dto.response;

public record WalkResumeResponse(
        Long walkId,
        String status,
        int totalPausedSeconds
) {
}
