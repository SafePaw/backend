package com.ne7k.safepaw.user.dto.response;

import com.ne7k.safepaw.user.domain.AuthProvider;
import com.ne7k.safepaw.user.domain.User;
import com.ne7k.safepaw.user.domain.UserRole;

import java.util.List;

public record MeResponse(
        Long id,
        String nickname,
        String email,
        AuthProvider primaryAuthProvider,
        UserRole role,
        boolean dogSetupRequired,
        List<MeDogSummary> dogs
) {
    public static MeResponse of(User user, boolean dogSetupRequired, List<MeDogSummary> dogs) {
        return new MeResponse(
                user.getId(),
                user.getNickname(),
                user.getEmail(),
                user.getPrimaryAuthProvider(),
                user.getRole(),
                dogSetupRequired,
                dogs
        );
    }
}
