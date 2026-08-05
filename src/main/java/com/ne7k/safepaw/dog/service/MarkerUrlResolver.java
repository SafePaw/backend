package com.ne7k.safepaw.dog.service;

import com.ne7k.safepaw.dog.domain.MarkerImageType;
import com.ne7k.safepaw.global.storage.MinioStorageClient;
import com.ne7k.safepaw.global.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarkerUrlResolver {

    private final MinioStorageClient storage;
    private final StorageProperties props;

    public record MarkerFields(MarkerImageType type, String value, String url) {}

    public String resolve(String markerKey) {
        return resolveFields(markerKey).url();
    }

    public MarkerFields resolveFields(String markerKey) {
        if (markerKey == null || markerKey.isBlank()) {
            return new MarkerFields(null, null, null);
        }
        if (markerKey.startsWith("presets/")) {
            String presetCode = markerKey
                    .replace("presets/markers/", "")
                    .replaceAll("\\.png$", "");
            String url = storage.publicUrl(
                    props.bucketPresets(),
                    markerKey.substring("presets/".length()));
            return new MarkerFields(MarkerImageType.PRESET, presetCode, url);
        }
        String url = storage.publicUrl(props.bucketMarkers(), markerKey);
        return new MarkerFields(MarkerImageType.UPLOADED, markerKey, url);
    }
}