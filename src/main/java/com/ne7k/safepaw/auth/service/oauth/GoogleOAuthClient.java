package com.ne7k.safepaw.auth.service.oauth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.ne7k.safepaw.auth.domain.SocialProvider;
import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.util.Map;

@Component
public class GoogleOAuthClient implements OAuthClient {

    private final WebClient tokenClient;
    private final GoogleIdTokenVerifier verifier;
    private final String clientId;
    private final String clientSecret;

    public GoogleOAuthClient(
            WebClient.Builder builder,
            @Value("${safepaw.oauth.google.token-uri}") String tokenUri,
            @Value("${safepaw.oauth.google.web-client-id}") String webClientId,
            @Value("${safepaw.oauth.google.client-secret}") String clientSecret
    ) {
        this.tokenClient = builder.baseUrl(tokenUri).build();
        this.clientId = webClientId;
        this.clientSecret = clientSecret;
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(webClientId))
                .build();
    }

    @Override
    public SocialProvider support() {
        return SocialProvider.GOOGLE;
    }

    @Override
    public OAuthUserInfo verifyWithCode(String authorizationCode, String redirectUri) {
        String idTokenStr = exchangeCodeForIdToken(authorizationCode, redirectUri);
        return verifyIdToken(idTokenStr);
    }

    @SuppressWarnings("unchecked")
    // POST oauth2.googleapis.com/token → 응답 id_token
    private String exchangeCodeForIdToken(String code, String redirectUri) {
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
                                            "구글 authorization code 교환 실패"))))
                    .onStatus(HttpStatusCode::is5xxServerError, res -> res.createException()
                            .flatMap(ex -> reactor.core.publisher.Mono.error(
                                    new BusinessException(ErrorCode.AUTH_GOOGLE_API_FAILED,
                                            "구글 token API 호출 실패"))))
                    .bodyToMono(Map.class)
                    .block();

            if (body == null || body.get("id_token") == null) {
                throw new BusinessException(ErrorCode.AUTH_INVALID_ID_TOKEN, "구글 id_token 누락");
            }
            return String.valueOf(body.get("id_token"));

        } catch (WebClientException e) {
            throw new BusinessException(ErrorCode.AUTH_GOOGLE_API_FAILED, "구글 token API 네트워크 오류");
        }
    }

    private OAuthUserInfo verifyIdToken(String idTokenStr) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenStr);
            if (idToken == null) {
                throw new BusinessException(ErrorCode.AUTH_INVALID_ID_TOKEN, "구글 idToken 서명 및 검증 실패");
            }
            GoogleIdToken.Payload payload = idToken.getPayload();
            String sub = payload.getSubject();
            String email = payload.getEmail();
            String nickname = Optional.ofNullable((String) payload.get("name")).orElse("google_" + sub);

            return new OAuthUserInfo(SocialProvider.GOOGLE, sub, email, nickname);

        } catch (GeneralSecurityException e) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_ID_TOKEN, "구글 서명 검증 실패");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AUTH_GOOGLE_API_FAILED, "구글 JWKS 호출 실패");
        }
    }

}
