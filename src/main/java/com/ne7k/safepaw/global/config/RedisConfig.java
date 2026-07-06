package com.ne7k.safepaw.global.config;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    // spring boot redis 연결 팩토리
    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory(RedisProperties props) {
        RedisStandaloneConfiguration cfg = new RedisStandaloneConfiguration(props.getHost(), props.getPort());
        return new LettuceConnectionFactory(cfg);
    }

    // String
    @Bean
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory cf) {
        return new StringRedisTemplate(cf);
    }
}
