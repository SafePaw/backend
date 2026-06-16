package com.ne7k.safepaw.dog.domain;

public enum DogRank {
    PUPPY_WALKER, // 새내기 산책러
    NEIGHBORHOOD_EXPLORER, // 동네 탐험가
    STREET_STROLLER, // 동네 산책꾼
    TERRITORY_PIONEER, // 영역 개척가
    ALPHA_DOG; // 알파 독

    public static DogRank initial() {
        return PUPPY_WALKER;
    }
}
