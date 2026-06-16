package com.ne7k.safepaw.global.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "safepaw.storage") // yml storage 읽어옴
public record StorageProperties(
        String provider,
        String region,
        String endpoint,
        String publicEndpoint,
        String accessKey,
        String secretKey,
        boolean pathStyleAccess,
        String bucketMarkers,
        String bucketPresets,
        String publicBaseUrl
) {
}
