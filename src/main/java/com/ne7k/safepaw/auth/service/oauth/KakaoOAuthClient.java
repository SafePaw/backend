package com.ne7k.safepaw.auth.service.oauth;

import com.ne7k.safepaw.auth.domain.SocialProvider;
import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import org.springframework.http.HttpHeaders;
import java.util.Map;
import java.util.Optional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;

@Component
public class KakaoOAuthClient implements OAuthClient {

    private final WebClient tokenClient;
    private final WebClient userInfoClient;
    private final String clientId;
    private final String clientSecret;

    public KakaoOAuthClient(
            WebClient.Builder builder,
            // value로 yml 값 읽어옴
            @Value("${safepaw.oauth.kakao.token-uri}") String tokenUri,
            @Value("${safepaw.oauth.kakao.user-info-uri}") String userInfoUri,
            @Value("${safepaw.oauth.kakao.client-id}") String clientId,
            @Value("${safepaw.oauth.kakao.client-secret}") String clientSecret
    ) {
        this.tokenClient = builder.baseUrl(tokenUri).build();
        this.userInfoClient = builder.baseUrl(userInfoUri).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public SocialProvider support() {
        return SocialProvider.KAKAO;
    }

    // code -> access token
    @Override
    public OAuthUserInfo verifyWithCode(String authorizationCode, String redirectUri) {
        String accessToken = exchangeCodeForAccessToken(authorizationCode, redirectUri);
        return verifyAccessToken(accessToken);
    }

    @SuppressWarnings("unchecked")
    //POST kauth/oauth/token (authorization_code)
    private String exchangeCodeForAccessToken(String code, String redirectUri) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "authorization_code");
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);
            form.add("redirect_uri", redirectUri);
            form.add("code", code);

            Map<String, Object> body = tokenClient.post()
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, res -> res.createException()
                            .flatMap(ex -> reactor.core.publisher.Mono.error(
                                    new BusinessException(ErrorCode.AUTH_INVALID_ID_TOKEN,
                                            "카카오 authorization code 교환 실패"))))
                    .onStatus(HttpStatusCode::is5xxServerError, res -> res.createException()
                            .flatMap(ex -> reactor.core.publisher.Mono.error(
                                    new BusinessException(ErrorCode.AUTH_KAKAO_API_FAILED,
                                            "카카오 token API 호출 실패"))))
                    .bodyToMono(Map.class)
                    .block();

            if (body == null || body.get("access_token") == null) {
                throw new BusinessException(ErrorCode.AUTH_INVALID_ID_TOKEN, "카카오 access_token 누락");
            }
            return String.valueOf(body.get("access_token"));

        } catch (WebClientException e) {
            // BusinessException 은 catch 없이 그대로 전파
            throw new BusinessException(ErrorCode.AUTH_KAKAO_API_FAILED, "카카오 token API 네트워크 오류");
        }
    }

    @SuppressWarnings("unchecked")
    // kakao user/me 파싱 동일
    private OAuthUserInfo verifyAccessToken(String accessToken) {
        try {
            Map<String, Object> body = userInfoClient.get()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, res -> res.createException()
                            .flatMap(ex -> reactor.core.publisher.Mono.error(
                                    new BusinessException(ErrorCode.AUTH_INVALID_ID_TOKEN,
                                            "카카오 accessToken 검증 실패"))))
                    .onStatus(HttpStatusCode::is5xxServerError, res -> res.createException()
                            .flatMap(ex -> reactor.core.publisher.Mono.error(
                                    new BusinessException(ErrorCode.AUTH_KAKAO_API_FAILED,
                                            "카카오 user/me 호출 실패"))))
                    .bodyToMono(Map.class)
                    .block();

            if (body == null || body.get("id") == null) {
                throw new BusinessException(ErrorCode.AUTH_INVALID_ID_TOKEN, "카카오 응답 형식 오류");
            }

            String providerUserId = String.valueOf(body.get("id"));
            Map<String, Object> kakaoAccount = (Map<String, Object>) body.getOrDefault("kakao_account", Map.of());
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.getOrDefault("profile", Map.of());

            String email = (String) kakaoAccount.get("email");
            String nickname = Optional.ofNullable((String) profile.get("nickname"))
                    .orElse("kakao_" + providerUserId);

            return new OAuthUserInfo(SocialProvider.KAKAO, providerUserId, email, nickname);

        } catch (WebClientException e) {
            throw new BusinessException(ErrorCode.AUTH_KAKAO_API_FAILED, "카카오 API 네트워크 오류");
        }
    }
}
