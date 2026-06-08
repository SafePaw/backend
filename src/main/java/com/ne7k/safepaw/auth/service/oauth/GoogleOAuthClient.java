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

@Component
public class GoogleOAuthClient implements OAuthClient {

    private final GoogleIdTokenVerifier verifier;

    public GoogleOAuthClient(@Value("${safepaw.oauth.google.web-client-id}") String webClientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(webClientId))
                .build();
    }

    @Override
    public SocialProvider support() {
        return SocialProvider.GOOGLE;
    }

    @Override
    public OAuthUserInfo verify(String idTokenStr) {
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
