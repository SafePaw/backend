package com.ne7k.safepaw.auth.dto.response;

import com.ne7k.safepaw.user.domain.AuthProvider;
import com.ne7k.safepaw.user.domain.User;

public record AuthUserResponse(
        Long id,
        String nickname,
        String email,
        AuthProvider primaryAuthProvider,
        boolean dogSetupRequired // 강아지 등록해야하는지
) {
    public static AuthUserResponse of(User u, boolean dogSetupRequired) {
        return new AuthUserResponse(
                u.getId(),
                u.getNickname(),
                u.getEmail(),
                u.getPrimaryAuthProvider(),
                dogSetupRequired
        );
    }
}
