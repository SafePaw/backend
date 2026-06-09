package com.ne7k.safepaw.auth.service.oauth;

import com.ne7k.safepaw.auth.domain.SocialProvider;

public interface OAuthClient {

    // 소셜 담당
    SocialProvider support();

    // authorization code로 변경
    OAuthUserInfo verifyWithCode(String authorizationCode, String redirectUri);
}
