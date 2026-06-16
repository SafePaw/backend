package com.ne7k.safepaw.dog.service;

import com.ne7k.safepaw.dog.domain.Dog;
import com.ne7k.safepaw.dog.domain.MarkerImageType;
import com.ne7k.safepaw.dog.dto.request.DogCreateRequest;
import com.ne7k.safepaw.dog.dto.request.DogUpdateRequest;
import com.ne7k.safepaw.dog.dto.response.DogResponse;
import com.ne7k.safepaw.dog.repository.DogRepository;
import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.global.storage.MinioStorageClient;
import com.ne7k.safepaw.global.storage.StorageProperties;
import com.ne7k.safepaw.user.domain.User;
import com.ne7k.safepaw.user.repsitory.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DogService {

    // 최대 강아지 5마리
    @Value("${safepaw.dog.max-per-user:5}")
    private int maxperUser;

    private final DogRepository dogRepository;
    private final UserRepository userRepository;
    private final MinioStorageClient storage;
    private final StorageProperties storageProperties;

    @Transactional(readOnly = true)
    public List<DogResponse> listMine(Long userId) {
        return dogRepository.findAllByOwner_IdOrderByIdAsc(userId).stream()
                .map(dog -> DogResponse.from(dog, storage, storageProperties))
                .toList();
    }

    public DogResponse create(Long userId, DogCreateRequest req) {
        // 강아지 5마리인지 체크
        if (dogRepository.countByOwner_Id(userId) >= maxperUser) {
            throw new BusinessException(ErrorCode.DOG_LIMIT_EXCEEDED);
        }

        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        String markerKey = resolveMarkerKey(req.markerImageType(), req.markerImageValue());
        Dog dog = dogRepository.save(Dog.create(
                owner,
                req.name(),
                req.breed(),
                req.gender(),
                req.age(),
                req.weightKg(),
                markerKey,
                req.territoryColor()));
        return DogResponse.from(dog, storage, storageProperties);

    }

    public DogResponse update(Long userId, Long dogId, DogUpdateRequest req) {
        Dog dog = dogRepository.findById(dogId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOG_NOT_FOUND));
        if (!dog.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.DOG_NOT_OWNED);
        }

        String markerKey = null;
        if (req.markerImageType() != null || req.markerImageValue() != null) {
            markerKey = resolveMarkerKey(req.markerImageType(), req.markerImageValue());
        }
        dog.updateProfile(
                req.name(),
                req.breed(),
                req.gender(),
                req.age(),
                req.weightKg(),
                markerKey,
                req.territoryColor());
        return DogResponse.from(dog, storage, storageProperties);
    }

    public void delete(Long userId, Long dogId) {
        Dog dog = dogRepository.findById(dogId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOG_NOT_FOUND));

        if(!dog.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.DOG_NOT_OWNED);
        }
        dogRepository.delete(dog);
    }

    private String resolveMarkerKey(MarkerImageType type, String value) {
        if(type == null || value == null || value.isBlank()) return null;
        if(type == MarkerImageType.PRESET) {
            return "presets/markers/" + value + ".png";
        }
        return value;
    }
}
