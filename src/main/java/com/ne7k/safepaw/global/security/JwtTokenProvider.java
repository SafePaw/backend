package com.ne7k.safepaw.global.security;

import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;

@Component
public class JwtTokenProvider {

    private final JwtProperties props;
    private final SecretKey accessKey;
    private final SecretKey refreshKey;

    public JwtTokenProvider(JwtProperties props) {
        this.props = props;
        this.accessKey = toKey(props.accessSecret());
        this.refreshKey = toKey(props.accessSecret());
    }

    // 서명 열쇠
    private SecretKey toKey(String secret) {
     byte[] bytes;
     try {
         bytes = Decoders.BASE64.decode(secret); // base64 decode
     } catch (Exception ignore) { // ignore로 예외 객체 미사용
         bytes = secret.getBytes(StandardCharsets.UTF_8);
     }
     return Keys.hmacShaKeyFor(bytes); // hmac-sha 서명용 키
    }

    // create access token
    public String issueAccessToken(Long userId) {
        Instant now = Instant.now(); // 현재 = 발급 시각
        Instant exp = now.plus(Duration.ofMinutes(props.accessExpirationMinutes())); // yml 값 가져와서 n분 길이의 통행증 발급
        return Jwts.builder()
                .subject(String.valueOf(userId)) // 토큰 주인
                .issuedAt(Date.from(now)) // 발급 시각
                .expiration(Date.from(exp)) // 만료 시각
                .signWith(accessKey) // 키
                .compact(); // 한줄 문자열로 반환
    }

    // create refresh token
    public String issueRefreshToken(Long userId) {
        Instant now = Instant.now();
        Instant exp = now.plus(Duration.ofDays(props.refreshExpirationDays()));
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(refreshKey)
                .compact();
    }

    // access token 검증
    public Long parseAccessUserId(String token) {
        return parse(token, accessKey);
    }

    // refresh token 검증
    public Long parseRefreshUserId(String token) {
        return parse(token, refreshKey);
    }

    // 검증 로직
    private Long parse(String token, SecretKey key) {
        try {
            Claims c = Jwts.parser() // 검증 시작
                    .verifyWith(key) // 키 확인
                    .build() // 검증기 완성
                    .parseSignedClaims(token) // 문자열 파싱 및 서명 검증
                    .getPayload(); // 퉤
            return Long.parseLong(c.getSubject());

        } catch (ExpiredJwtException e) { // 만료
            throw new BusinessException(ErrorCode.AUTH_EXPIRED_REFRESH, "토큰이 만료되었습니다.");

        } catch (JwtException | IllegalArgumentException e) { // 그 외
            throw new BusinessException(ErrorCode.COMMON_UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }
    }

    // 리프레시 토큰 만료일 갱신
    public long refreshExpirationDays() {
        return props.refreshExpirationDays();
    }

    // refresh token을 db에 평문으로 넣지 않고 sha-256 해시 문자열로 저장
    public String hashForStorage(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256"); // 해시 제작
            byte[] digest = md.digest(token.getBytes(StandardCharsets.UTF_8)); // 같은 토큰은 같은 해시, 16진수로
            return HexFormat.of().formatHex(digest); // varchar로 교체

        } catch (NoSuchAlgorithmException e) { // sha-256
            throw new IllegalStateException(e);
        }
    }

}
