package com.ne7k.safepaw.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.global.response.ApiResponse;
import com.ne7k.safepaw.global.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ObjectMapper objectMapper
            ) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // jwt 사용으로 인한 csrf 차단
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // cors
                .sessionManagement(s -> s // 세션 미사용
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth // 검사 제외 url
                        .requestMatchers("/").permitAll() // 루트 경로 추가!
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/ping/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class) // jwt 먼저 실행
                .exceptionHandling(eh -> eh // exception handler 객체 생성 및 설정
                        // 인증 안 됨
                        .authenticationEntryPoint((req, rsp, e) -> { // 요청, 응답, 예외
                            rsp.setStatus(401);
                            rsp.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            rsp.setCharacterEncoding("UTF-8");
                            objectMapper.writeValue(
                                    rsp.getWriter(),
                                    ApiResponse.fail(ErrorCode.COMMON_UNAUTHORIZED, "인증이 필요합니다.")
                            );
                        })
                        // 권한 커트
                        .accessDeniedHandler((req, rsp, e) -> {
                            rsp.setStatus(403);
                            rsp.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            rsp.setCharacterEncoding("UTF-8");
                            objectMapper.writeValue(
                                    rsp.getWriter(),
                                    ApiResponse.fail(ErrorCode.COMMON_FORBIDDEN, "권한이 없습니다.")
                            );
                        })

                );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // localhost 5173
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "https://ne7k.cloud",
                "https://safepaw-frontend.vercel.app",
                "https://frontend-git-preview-leehaeiins-projects.vercel.app"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
