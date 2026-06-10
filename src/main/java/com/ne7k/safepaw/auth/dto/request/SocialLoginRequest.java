package com.ne7k.safepaw.auth.dto.request;

import com.ne7k.safepaw.auth.domain.SocialProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// swagger ui api 설명 표시
@Schema(description = "소셜 로그인 요청 - authorization code")
public record SocialLoginRequest(

        @Schema(example = "KAKAO")
        @NotNull SocialProvider provider,

        @Schema(description = "IDP redirect 후에 callback url code parameter")
        @NotBlank String authorizationCode,

        @Schema(description = "authorization 요청 시 사용한 redirect uri")
        @NotBlank String redirectUri

) {
}
