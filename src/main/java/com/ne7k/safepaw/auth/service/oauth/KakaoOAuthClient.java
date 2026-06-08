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

@Component
public class KakaoOAuthClient implements OAuthClient {

    private final WebClient webClient;

    public KakaoOAuthClient(
            WebClient.Builder builder,
            // value로 yml 값 읽어옴
            @Value("${safepaw.oauth.kakao.user-info-uri}") String userInfoUri
            ) {
        this.webClient = builder.baseUrl(userInfoUri).build();
    }

    @Override
    public SocialProvider support() {
        return SocialProvider.KAKAO;
    }

    // get 요청
    @Override
    @SuppressWarnings("unchecked") // 캐스팅시 컴파일 끄기
    public OAuthUserInfo verify(String accessToken) {
        try {
            Map<String, Object> body = webClient.get()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve() // 실행 단계
                    // 상황별 에러
                    .onStatus(HttpStatusCode::is4xxClientError,
                            res -> res.createException()
                                    // reactor 비동기 라이브러리, mono 박스
                                    .flatMap(ex -> reactor.core.publisher.Mono.error(
                                            new BusinessException(ErrorCode.AUTH_INVALID_ID_TOKEN,
                                                    "카카오 access Token 검증 실패")
                                    ))
                    )
                    .onStatus(HttpStatusCode::is5xxServerError,
                            res -> res.createException()
                                    .flatMap(ex -> reactor.core.publisher.Mono.error(
                                            new BusinessException(ErrorCode.AUTH_KAKAO_API_FAILED,
                                                    "카카오 API 호출 실패")
                                    )))
                    .bodyToMono(Map.class) // map class 타입 변환
                    .block();

            if (body == null || body.get("id") == null) {
                throw new BusinessException(ErrorCode.AUTH_INVALID_ID_TOKEN, "카카오 응답 형식 오류");
            }

            String providerUserId = String.valueOf(body.get("id"));
            Map<String, Object> kakaoAccount = (Map<String, Object>) body.getOrDefault("kakao_account", Map.of());
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.getOrDefault("profile", Map.of());

            String email = (String) kakaoAccount.get("email");
            String nickname = Optional.ofNullable((String) kakaoAccount.get("nickname")).orElse("kakao_" + providerUserId);

            // response
            return new OAuthUserInfo(SocialProvider.KAKAO, providerUserId, email, nickname);

        } catch (BusinessException e){
            throw e;
        } catch (WebClientException e) {
            throw new BusinessException(ErrorCode.AUTH_KAKAO_API_FAILED, "카카오 API 호출 실패");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AUTH_KAKAO_API_FAILED, "카카오 API 네트워크 오류");
        }
    }
}
