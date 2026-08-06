package com.ne7k.safepaw.dog.dto.response;

import com.ne7k.safepaw.dog.domain.Dog;
import com.ne7k.safepaw.dog.domain.DogRank;
import com.ne7k.safepaw.dog.domain.Gender;
import com.ne7k.safepaw.dog.domain.MarkerImageType;
import com.ne7k.safepaw.dog.service.MarkerUrlResolver;
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

    /** 기존 시그니처 유지 — DogService 호출부 변경 최소화 */
    public static DogResponse from(Dog dog, MinioStorageClient storage, StorageProperties props) {
        MarkerUrlResolver.MarkerFields marker =
                new MarkerUrlResolver(storage, props).resolveFields(dog.getMarkerImageKey());
        return of(dog, marker);
    }

    public static DogResponse from(Dog dog, MarkerUrlResolver resolver) {
        return of(dog, resolver.resolveFields(dog.getMarkerImageKey()));
    }

    private static DogResponse of(Dog dog, MarkerUrlResolver.MarkerFields marker) {
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
}