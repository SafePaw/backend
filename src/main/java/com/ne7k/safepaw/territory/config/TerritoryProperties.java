package com.ne7k.safepaw.territory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "safepaw.territory")
public record TerritoryProperties(
        double loopCloseMeters,
        int minPoints,
        double minWidthMeters,
        double minAreaSquareMeters,
        double concaveHullTargetPercent,
        double hullSimplifyToleranceDegrees,
        double bboxMaxAreaSquareMeters,
        int duplicateWindowHours
) {
}
