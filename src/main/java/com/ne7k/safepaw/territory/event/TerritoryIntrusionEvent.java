package com.ne7k.safepaw.territory.event;

public record TerritoryIntrusionEvent(
        Long victimUserId,
        Long intrusionId,
        Long victimTerritoryId,
        Long victimDogId,
        String victimDogName,
        Long intruderTerritoryId,
        Long intruderDogId,
        String intruderDogName,
        double overlapRatio,
        double stolenAreaSquareMeters,
        double remainderAreaSquareMeters,
        String victimStatusAfter
) {}