package com.ne7k.safepaw.auth.dto.response;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        AuthUserResponse user
) {
}
