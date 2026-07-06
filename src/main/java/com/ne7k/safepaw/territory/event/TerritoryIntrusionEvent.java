package com.ne7k.safepaw.territory.event;

public record TerritoryIntrusionEvent(
        Long victimUserId,
        Long victimTerritoryId,
        Long intruderTerritoryId,
        String intruderDogName,
        double overlapRatio
) {}