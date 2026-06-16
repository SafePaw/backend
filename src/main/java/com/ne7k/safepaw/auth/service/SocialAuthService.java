package com.ne7k.safepaw.auth.service;

import com.ne7k.safepaw.auth.domain.SocialIdentity;
import com.ne7k.safepaw.auth.domain.SocialProvider;
import com.ne7k.safepaw.auth.dto.request.SocialLoginRequest;
import com.ne7k.safepaw.auth.dto.response.AuthTokenResponse;
import com.ne7k.safepaw.auth.dto.response.AuthUserResponse;
import com.ne7k.safepaw.auth.repository.SocialIdentityRepository;
import com.ne7k.safepaw.auth.service.oauth.OAuthClient;
import com.ne7k.safepaw.auth.service.oauth.OAuthClientResolver;
import com.ne7k.safepaw.auth.service.oauth.OAuthUserInfo;
import com.ne7k.safepaw.dog.service.DogSetupRequiredChecker;
import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.global.security.JwtTokenProvider;
import com.ne7k.safepaw.user.domain.User;
import com.ne7k.safepaw.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SocialAuthService {

    private final OAuthClientResolver oAuthClientResolver;
    private final UserRepository userRepository;
    private final SocialIdentityRepository socialIdentityRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final DogSetupRequiredChecker dogSetupRequiredChecker;

    // 토큰 검증 및 초기 설정
    public AuthTokenResponse loginOrSignup(SocialLoginRequest req, String userAgent) {
        SocialProvider provider = req.provider(); // 종류
        String code = req.authorizationCode(); // authorization code
        String redirectUri = req.redirectUri();

        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "authorizationCode가 비어있습니다.");
        }

        if (redirectUri == null || redirectUri.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "redirectUri가 비어있습니다.");
        }

        OAuthClient client = oAuthClientResolver.resolve(provider);
        OAuthUserInfo info = client.verifyWithCode(code, redirectUri); // 검증

        User user = socialIdentityRepository
                .findByProviderAndProviderUserId(provider, info.providerUserId()) // 아이디 존재하면, 로그인
                .map(SocialIdentity::getUser)
                .orElseGet(() -> createNewSocialUser(info)); // 회원 가입 로직

        user.updateLastLoginAt(); // 마지막 로그인 시각 갱신

        String accessToken = jwtTokenProvider.issueAccessToken(user.getId());
        String refreshToken = refreshTokenService.issueAndStore(user, userAgent);

        boolean dogSetupRequired = dogSetupRequiredChecker.isRequired(user.getId());

        return new AuthTokenResponse( // response dto
                accessToken,
                refreshToken,
                AuthUserResponse.of(user, dogSetupRequired)
        );
    }

    // 계정 생성
    public User createNewSocialUser(OAuthUserInfo info) {
        String nickname = ensureUniqueNickname(info.nickname());
        User user = User.createSocial(
                info.provider().toAuthProvider(),
                info.email(),
                nickname
        );
        user = userRepository.save(user);

        SocialIdentity socialIdentity = SocialIdentity.builder()
                .user(user)
                .provider(info.provider())
                .providerUserId(info.providerUserId())
                .build();
        socialIdentityRepository.save(socialIdentity);
        log.info("신규 소셜 가입 : userId={}, provider={}", user.getId(), info.provider());
        return user;

    }

    // 닉네임 중복 시 suffix
    public String ensureUniqueNickname(String base) {

        String cleaned = (base == null || base.isBlank()) ? "user" : base; // 기본값 user, 있으면 base 사용
        if (cleaned.length() > 16) cleaned = cleaned.substring(0, 16); // 16자 이상은 자름
        if (cleaned.length() < 2) cleaned = cleaned + "00"; // 2자리는 00 붙임

        String candidate = cleaned;
        for (int i = 0; i < 10; i++) {
            if(!userRepository.existsByNickname(candidate)) return candidate;
            candidate = cleaned + ThreadLocalRandom.current().nextInt(1000, 9999); // 중복이면 랜덤 숫자
        }
        throw new BusinessException(ErrorCode.USER_NICKNAME_DUPLICATED,
                "닉네임 자동 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");
    }
}
