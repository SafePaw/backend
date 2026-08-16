package com.ne7k.safepaw;

import com.ne7k.safepaw.global.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAsync
@SpringBootApplication
//@EnableConfigurationProperties(JwtProperties.class)
@ConfigurationPropertiesScan // bean 등록
public class SafepawApplication {

	public static void main(String[] args) {
		SpringApplication.run(SafepawApplication.class, args);
	}

}
