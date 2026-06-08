package com.ne7k.safepaw.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.global.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter { // 한 번만 진행

    private static final String AUTHORIZATION_HEADER = "Authorization"; // http 헤더
    private static final String BEARER = "Bearer "; // 토큰 앞 접두사

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper; // java -> json

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, ObjectMapper objectMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request, //jwt token
            @NonNull HttpServletResponse response, // 위조
            @NonNull FilterChain chain

    ) throws ServletException, IOException {
        String header = request.getHeader(AUTHORIZATION_HEADER); // authoriztion header

        if (header != null && header.startsWith(BEARER)) { // bearer로 시작하나요
            String token = header.substring(BEARER.length()).trim(); // bearer 뒤만 token으로 뺌
            try {
                Long userId = jwtTokenProvider.parseAccessUserId(token);
                CustomUserDetails principal = new CustomUserDetails(userId, "USER"); // principal 객체
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()); // 명찰, 비번, 권한
                SecurityContextHolder // 전역 보관함
                        .getContext()
                        .setAuthentication(auth); // 로그인 상태 저장
            } catch (BusinessException e) {
                writeError(response, e.getErrorCode(), e.getMessage());
                return;
            }
        }

        chain.doFilter(request, response); // 다음 필터로
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(errorCode.getStatus().value()); // errorcode
        response.setContentType(MediaType.APPLICATION_JSON_VALUE); // json
        response.setCharacterEncoding("UTF-8"); // 한글
        objectMapper.writeValue( // java -> json 에러코드 및 메시지
                response.getWriter(),
                ApiResponse.fail(errorCode, message)
        );
    }
}
