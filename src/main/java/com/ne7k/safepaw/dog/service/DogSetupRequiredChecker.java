package com.ne7k.safepaw.dog.service;

import com.ne7k.safepaw.dog.repository.DogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DogSetupRequiredChecker {

    private final DogRepository dogRepository;

    // 강아지 0 true
    public boolean isRequired(Long userId) {
        return !dogRepository.existsByOwner_Id(userId);
    }
}
