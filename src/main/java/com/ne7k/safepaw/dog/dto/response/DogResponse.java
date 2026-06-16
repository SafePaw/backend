package com.ne7k.safepaw.dog.dto.response;

import com.ne7k.safepaw.dog.domain.Dog;
import com.ne7k.safepaw.dog.domain.DogRank;
import com.ne7k.safepaw.dog.domain.Gender;
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
        String territoryColor,
        DogRank rank,
        int totalXp
) {

    public static DogResponse from(Dog dog, MinioStorageClient storage, StorageProperties props) {
        return new DogResponse(
                dog.getId(),
                dog.getName(),
                dog.getBreed(),
                dog.getGender(),
                dog.getAge(),
                dog.getWeightKg(),
                resolveMarkerUrl(dog.getMarkerImageKey(), storage, props),
                dog.getTerritoryColor(),
                dog.getRank(),
                dog.getTotalXp()
        );
    }

    private static String resolveMarkerUrl(String markerKey, MinioStorageClient storage, StorageProperties props) {
        if(markerKey == null || markerKey.isBlank()) return null;
        if(markerKey.startsWith("presets/")) {
            String objectKey = markerKey.substring("presets/".length());


            return storage.publicUrl(props.bucketPresets(), objectKey);
        }
        return storage.publicUrl(props.bucketMarkers(), markerKey);
    }
}
