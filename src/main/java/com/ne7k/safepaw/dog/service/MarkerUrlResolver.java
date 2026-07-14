package com.ne7k.safepaw.dog.service;

import com.ne7k.safepaw.global.storage.MinioStorageClient;
import com.ne7k.safepaw.global.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarkerUrlResolver {

    private final MinioStorageClient storage;
    private final StorageProperties props;

    // db 저장된 마커 키 minio 공개 url 변환
    public String resolve(String markerKey) {
        if (markerKey == null || markerKey.isBlank()) {
            return null;
        }
        if (markerKey.startsWith("presets/")) {
            return storage.publicUrl(
                    props.bucketPresets(),
                    markerKey.substring("presets/".length()));
        }
        return storage.publicUrl(props.bucketMarkers(), markerKey);
    }

}
