package com.ne7k.safepaw.auth.dto.request;

import com.ne7k.safepaw.auth.domain.SocialProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

// swagger ui api 설명 표시
@Schema(description = "소셜 로그인 요청 - kakao/naver은 access token, google은 idToken 사용함")
public record SocialLoginRequest(

        @Schema(example = "KAKAO")
        @NotNull SocialProvider provider,

        @Schema(description = "kakao/naver이 발급한 access token")
        String accessToken,

        @Schema(description = "google이 발급한 idToken")
        String idToken

) {

    public String tokenForProvider() {
        return provider == SocialProvider.GOOGLE ? idToken : accessToken;
    }
}
