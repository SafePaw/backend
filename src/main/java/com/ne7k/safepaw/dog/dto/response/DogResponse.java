package com.ne7k.safepaw.dog.dto.response;

import com.ne7k.safepaw.dog.domain.Dog;
import com.ne7k.safepaw.dog.domain.DogRank;
import com.ne7k.safepaw.dog.domain.Gender;
import com.ne7k.safepaw.dog.domain.MarkerImageType;
import com.ne7k.safepaw.global.storage.MinioStorageClient;
import com.ne7k.safepaw.global.storage.StorageProperties;

public record DogResponse(
        Long id,
        String name,
        String breed,
        Gender gender,
        Integer age,
        java.math.BigDecimal weightKg,
        String markerImageUrl,
        MarkerImageType markerImageType,
        String markerImageValue,
        String territoryColor,
        DogRank rank,
        int totalXp
) {

    public static DogResponse from(Dog dog, MinioStorageClient storage, StorageProperties props) {
        String key = dog.getMarkerImageKey();
        MarkerFields marker = resolveMarkerFields(key, storage, props);
        return new DogResponse(
                dog.getId(),
                dog.getName(),
                dog.getBreed(),
                dog.getGender(),
                dog.getAge(),
                dog.getWeightKg(),
                marker.url(),
                marker.type(),
                marker.value(),
                dog.getTerritoryColor(),
                dog.getRank(),
                dog.getTotalXp()
        );
    }

    private record MarkerFields(MarkerImageType type, String value, String url) {}

    private static MarkerFields resolveMarkerFields(String markerKey,
                                                    MinioStorageClient storage,
                                                    StorageProperties props) {
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