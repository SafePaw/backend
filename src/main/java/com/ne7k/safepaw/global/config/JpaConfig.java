package com.ne7k.safepaw.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration // 설정 파일 명시
@EnableJpaAuditing //시간 자동 기록
public class JpaConfig {
}
