package com.ne7k.safepaw.territory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "safepaw.territory")
public record TerritoryProperties(
        double loopCloseMeters,
        int minPoints,
        double minWidthMeters,
        double minAreaSquareMeters,
        double pathSimplifyToleranceDegrees,
        double polygonSimplifyToleranceDegrees,
        double minPolygonPartAreaSquareMeters,
        double bboxMaxAreaSquareMeters,
        int duplicateWindowHours
) {
}