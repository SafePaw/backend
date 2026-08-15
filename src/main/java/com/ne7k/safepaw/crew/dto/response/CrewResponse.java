package com.ne7k.safepaw.crew.dto.response;

import com.ne7k.safepaw.crew.domain.Crew;

public record CrewResponse(
        Long id,
        String name,
        String imageUrl,
        String territoryColor,
        int memberCount,
        int maxMembers,
        Long leaderUserId,
        String inviteCode,
        String myRole
) {
    public static CrewResponse of(Crew crew, String imageUrl, int memberCount,
                                  int maxMembers, String inviteCode, String myRole) {
        return new CrewResponse(
                crew.getId(),
                crew.getName(),
                imageUrl,
                crew.getTerritoryColor(),
                memberCount,
                maxMembers,
                crew.getLeader().getId(),
                inviteCode,
                myRole
        );
    }
}
