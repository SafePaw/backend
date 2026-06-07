package com.ne7k.safepaw.auth.domain;

import com.ne7k.safepaw.user.domain.AuthProvider;

public enum SocialProvider {
    KAKAO, NAVER, GOOGLE;

    // 각 소셜 로그인 스위칭
    public AuthProvider toAuthProvider() {
        return switch (this) {
            case KAKAO -> AuthProvider.KAKAO;
            case NAVER -> AuthProvider.NAVER;
            case GOOGLE -> AuthProvider.GOOGLE;
        };
    }
}
