package com.ne7k.safepaw.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter { // 한 번만 진행

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request, //jwt token
            @NonNull HttpServletResponse response, // 위조
            @NonNull FilterChain filterChain // 다음 필터체인
    ) throws ServletException, IOException {
        filterChain.doFilter(request, response); // 다음 필터로
    }
}
