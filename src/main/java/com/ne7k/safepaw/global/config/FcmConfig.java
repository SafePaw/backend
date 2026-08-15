package com.ne7k.safepaw.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Base64;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "safepaw.fcm.enabled", havingValue = "true")
public class FcmConfig {

    @Bean
    FirebaseApp firebaseApp(
            @Value("${safepaw.fcm.credentials-path:}") String credentialsPath,
            @Value("${safepaw.fcm.credentials-json-base64:}") String credentialsJsonBase64
    ) throws Exception {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        try (InputStream in = openCredentials(credentialsPath, credentialsJsonBase64)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in))
                    .build();
            log.info("[FCM] FirebaseApp initialized");
            return FirebaseApp.initializeApp(options);
        }
    }

    @Bean
    FirebaseMessaging firebaseMessaging(FirebaseApp app) {
        return FirebaseMessaging.getInstance(app);
    }

    private InputStream openCredentials(String path, String base64) throws Exception {
        if (base64 != null && !base64.isBlank()) {
            byte[] decoded = Base64.getDecoder().decode(base64.trim());
            return new ByteArrayInputStream(decoded);
        }
        if (path != null && !path.isBlank()) {
            return new FileInputStream(path);
        }
        throw new IllegalStateException(
                "FCM credentials missing : set safepaw.fcm.credentials-json-base64 or credentials-path");
    }
}
