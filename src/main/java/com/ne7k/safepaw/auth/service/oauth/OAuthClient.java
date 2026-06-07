package com.ne7k.safepaw.auth.service.oauth;

import com.ne7k.safepaw.auth.domain.SocialProvider;

public interface OAuthClient {

    // 소셜 담당
    SocialProvider support();

    // 토큰 검증 후 소셜 유저 정보 반환
    OAuthUserInfo verify(String Token);
}
