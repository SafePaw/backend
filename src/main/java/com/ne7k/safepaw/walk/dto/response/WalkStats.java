package com.ne7k.safepaw.walk.dto.response;

public record WalkStats(
        double distanceMeters, int durationSeconds,
        double averageSpeedKmh, int pointCount, Double loopGapMeters
) {}