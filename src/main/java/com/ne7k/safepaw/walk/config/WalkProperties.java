package com.ne7k.safepaw.walk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "safepaw.walk")
public record WalkProperties(Gps gps, Batch batch, Session session) {

    public record Gps(double maxAccuracyMeters, double minSpeedKmh, double maxSpeedKmh,
                      double jumpDistanceMeters, int jumpMinIntervalSeconds) {}

    public record Batch(int maxPoints) {}

    public record Session(int minDurationSeconds, int lockTtlHours, int bufferTtlHours) {}
}
