package com.ne7k.safepaw.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

// *.yml 설정 병합
@ConfigurationProperties(prefix = "safepaw.jwt")
public record JwtProperties(
        String accessSecret,
        String refreshSecret,
        long accessExpirationMinutes,
        long refreshExpirationDays
) {

}
