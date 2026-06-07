package com.ne7k.safepaw.auth.service;

import com.ne7k.safepaw.auth.domain.RefreshToken;
import com.ne7k.safepaw.auth.dto.response.AuthUserResponse;
import com.ne7k.safepaw.auth.repository.RefreshTokenRepository;
import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.global.security.JwtTokenProvider;
import com.ne7k.safepaw.user.domain.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;

// jwt 발급 db 기록 갱신 및 폐기
@Service
@RequiredArgsConstructor // final 생성자
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    // 새 토큰 발급, db에는 hash 저장, client 평문 토큰 전송
    public String issueAndStore(User user, String userAgent) {
        String token = jwtTokenProvider.issueRefreshToken(user.getId());
        OffsetDateTime now = OffsetDateTime.now();

        RefreshToken row = RefreshToken.builder()
                .user(user)
                .tokenHash(jwtTokenProvider.hashForStorage(token))
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofDays(jwtTokenProvider.refreshExpirationDays())))
                .userAgent(userAgent)
                .build();
        refreshTokenRepository.save(row);

        return token;
    }

    // refresh 토큰 db 검색 -> 문제 시 차단 -> 정상이면 jwt 2개 발급 + db 갱신 -> old row 폐기
    public RotateResult rotate(String oldRefreshToken, String userAgent) {
        Long userId = jwtTokenProvider.parseRefreshUserId(oldRefreshToken);

        String oldHash = jwtTokenProvider.hashForStorage(oldRefreshToken); // jwt 평문
        RefreshToken row = refreshTokenRepository.findByTokenHash(oldHash) // 테이블에서 검색
                .orElseThrow( () -> new BusinessException(ErrorCode.AUTH_EXPIRED_REFRESH,
                        "DB에서 Refresh 토큰을 찾을 수 없습니다."));

        OffsetDateTime now = OffsetDateTime.now();

        if (row.isRevoked()) {
            refreshTokenRepository.revokeAllByUserId(userId, now);
            throw new BusinessException(ErrorCode.AUTH_REUSED_REFRESH,
                    "Refresh 토큰 재사용이 감지되어 모든 세션 종료합니다.");
        }
        if (!row.isActive(now)) {
            throw new BusinessException(ErrorCode.AUTH_EXPIRED_REFRESH,
                    "만료된 Refresh 토큰입니다.");
        }

        User user = row.getUser();
        // jwt 발급
        String newAccessToken = jwtTokenProvider.issueAccessToken(user.getId());
        String newRefreshToken = jwtTokenProvider.issueRefreshToken(user.getId());

        // 새로운 객체
        RefreshToken newRow = RefreshToken.builder()
                .user(user)
                .tokenHash(jwtTokenProvider.hashForStorage(newRefreshToken)) // db에 hash만
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofDays(jwtTokenProvider.refreshExpirationDays())))
                .userAgent(userAgent)
                .build();
        refreshTokenRepository.save(newRow);
        row.replaceWith(newRow.getId(), now); // insert

        AuthUserResponse userResponse = AuthUserResponse.of(user, true);
        return new RotateResult(newAccessToken, newRefreshToken, userResponse);
    }

    // refresh 세션 종료
    public void revokeAll(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId, OffsetDateTime.now());
    }

    // 결과 묶음
    public record RotateResult(String accessToken, String refreshToken, AuthUserResponse user) {}
}
