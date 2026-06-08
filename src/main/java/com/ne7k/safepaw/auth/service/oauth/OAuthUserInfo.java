package com.ne7k.safepaw.auth.service.oauth;

import com.ne7k.safepaw.auth.domain.SocialProvider;

// 형태 맞춰주기
public record OAuthUserInfo(
        SocialProvider provider,
        String providerUserId,
        String email,
        String nickname
) {
}
