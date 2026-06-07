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
import java.util.Map;
import java.util.Optional;

@Component
public class NaverOAuthClient implements OAuthClient{

    private final WebClient webClient;

    public NaverOAuthClient(
            WebClient.Builder builder,
            @Value("${safepaw.oauth.naver.user-info-uri}") String userInfoUri
            ) {
        this.webClient = builder.baseUrl(userInfoUri).build();
    }

    @Override
    public SocialProvider support() {
        return SocialProvider.NAVER;
    }

    @Override
    @SuppressWarnings("unchecked") // json -> map을 변환하는 과정에서 발생하는 오류 방지
    public OAuthUserInfo verify(String accessToken) {
        try {
            Map<String, Object> body = webClient.get()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken) // 헤더
                    .accept(MediaType.APPLICATION_JSON) // 형식
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, res -> res.createException().flatMap(
                            ex -> reactor.core.publisher.Mono.error(
                                    new BusinessException(ErrorCode.AUTH_INVALID_ID_TOKEN, "네이버 accessToken 검증 실패")
                            )
                    ))
                    .onStatus(HttpStatusCode::is5xxServerError, res -> res.createException().flatMap(
                            ex -> reactor.core.publisher.Mono.error(
                                    new BusinessException(ErrorCode.AUTH_NAVER_API_FAILED, "네이버 API 호출 실패")
                            )
                    ))
                    .bodyToMono(Map.class) // json -> map
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
            String providerUserid = (String) response.get("id");
            if (providerUserid == null) {
                throw new BusinessException(ErrorCode.AUTH_INVALID_ID_TOKEN, "네이버 id 누락");
            }
            String email = (String) response.get("email");
            String nickname = Optional.ofNullable((String) response.get("nickname")).orElse("naver_" + providerUserid);

            return new OAuthUserInfo(SocialProvider.NAVER, providerUserid, email, nickname);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AUTH_NAVER_API_FAILED, "네이버 API 네트워크 오류");
        }
    }
}
