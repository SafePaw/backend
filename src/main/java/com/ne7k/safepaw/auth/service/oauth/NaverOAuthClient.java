package com.ne7k.safepaw.auth.service.oauth;

import com.ne7k.safepaw.auth.domain.SocialProvider;
import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

@Component
public class NaverOAuthClient implements OAuthClient{

    private final WebClient tokenClient;
    private final WebClient userInfoClient;
    private final String clientId;
    private final String clientSecret;

    public NaverOAuthClient(
            WebClient.Builder builder,
            @Value("${safepaw.oauth.naver.token-uri}") String tokenUri,
            @Value("${safepaw.oauth.naver.user-info-uri}") String userInfoUri,
            @Value("${safepaw.oauth.naver.client-id}") String clientId,
            @Value("${safepaw.oauth.naver.client-secret}") String clientSecret
    ) {
        this.tokenClient = builder.baseUrl(tokenUri).build();
        this.userInfoClient = builder.baseUrl(userInfoUri).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public SocialProvider support() {
        return SocialProvider.NAVER;
    }

    @Override
    public OAuthUserInfo verifyWithCode(String authorizationCode, String redirectUri) {
        String accessToken = exchangeCodeForAccessToken(authorizationCode, redirectUri);
        return verifyAccessToken(accessToken);
    }

    @SuppressWarnings("unchecked")
    private String exchangeCodeForAccessToken(String code, String redirectUri) {
        try {
            String uri = UriComponentsBuilder.fromPath("")
                    .queryParam("grant_type", "authorization_code")
                    .queryParam("client_id", clientId)
                    .queryParam("client_secret", clientSecret)
                    .queryParam("redirect_uri", redirectUri)
                    .queryParam("code", code)
                    .build(true)
                    .toUriString();

            Map<String, Object> body = tokenClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, res -> res.createException()
                            .flatMap(ex -> reactor.core.publisher.Mono.error(
                                    new BusinessException(ErrorCode.AUTH_INVALID_ID_TOKEN,
                                            "네이버 authorization code 교환 실패"))))
                    .onStatus(HttpStatusCode::is5xxServerError, res -> res.createException()
                            .flatMap(ex -> reactor.core.publisher.Mono.error(
                                    new BusinessException(ErrorCode.AUTH_NAVER_API_FAILED,
                                            "네이버 token API 호출 실패"))))
                    .bodyToMono(Map.class)
                    .block();

            if (body == null || body.get("access_token") == null) {
                throw new BusinessException(ErrorCode.AUTH_INVALID_ID_TOKEN, "네이버 access_token 누락");
            }
            return String.valueOf(body.get("access_token"));

        } catch (WebClientException e) {
            throw new BusinessException(ErrorCode.AUTH_NAVER_API_FAILED, "네이버 token API 네트워크 오류");
        }
    }

    @SuppressWarnings("unchecked")
    // nid/me 파싱 동일
    private OAuthUserInfo verifyAccessToken(String accessToken) {
        try {
            Map<String, Object> body = userInfoClient.get()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, res -> res.createException()
                            .flatMap(ex -> reactor.core.publisher.Mono.error(
                                    new BusinessException(ErrorCode.AUTH_INVALID_ID_TOKEN,
                                            "네이버 accessToken 검증 실패"))))
                    .onStatus(HttpStatusCode::is5xxServerError, res -> res.createException()
                            .flatMap(ex -> reactor.core.publisher.Mono.error(
                                    new BusinessException(ErrorCode.AUTH_NAVER_API_FAILED,
                                            "네이버 API 호출 실패"))))
                    .bodyToMono(Map.class)
                    .block();

            if (body == null) {
                throw new BusinessException(ErrorCode.AUTH_INVALID_ID_TOKEN, "네이버 응답 비어있음");
            }

            String resultCode = String.valueOf(body.get("resultcode"));
            if (!"00".equals(resultCode)) {
                throw new BusinessException(ErrorCode.AUTH_INVALID_ID_TOKEN,
                        "네이버 resultcode != 00 : " + resultCode);
            }

            Map<String, Object> response = (Map<String, Object>) body.getOrDefault("response", Map.of());
            String providerUserId = (String) response.get("id");
            if (providerUserId == null) {
                throw new BusinessException(ErrorCode.AUTH_INVALID_ID_TOKEN, "네이버 id 누락");
            }

            String email = (String) response.get("email");
            String nickname = Optional.ofNullable((String) response.get("nickname"))
                    .orElse("naver_" + providerUserId);

            return new OAuthUserInfo(SocialProvider.NAVER, providerUserId, email, nickname);

        } catch (WebClientException e) {
            throw new BusinessException(ErrorCode.AUTH_NAVER_API_FAILED, "네이버 API 네트워크 오류");
        }
    }
}
