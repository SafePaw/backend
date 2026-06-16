package com.ne7k.safepaw.user.service;

import com.ne7k.safepaw.dog.repository.DogRepository;
import com.ne7k.safepaw.dog.service.DogSetupRequiredChecker;
import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.user.domain.User;
import com.ne7k.safepaw.user.dto.response.MeDogSummary;
import com.ne7k.safepaw.user.dto.response.MeResponse;
import com.ne7k.safepaw.user.repsitory.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserQueryService {

    private final UserRepository userRepository;
    private final DogRepository dogRepository;
    private final DogSetupRequiredChecker dogSetupRequiredChecker;

    @Transactional(readOnly = true)
    public MeResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        var dogs = dogRepository.findAllByOwner_IdOrderByIdAsc(userId).stream()
                .map(d -> new MeDogSummary(
                        d.getId(),
                        d.getName(),
                        d.getGender(),
                        d.getRank(),
                        d.getTotalXp(),
                        d.getTerritoryColor()
                )).toList();

        return MeResponse.of(user, dogSetupRequiredChecker.isRequired(userId), dogs);
    }

    public MeResponse updateNickname(Long userId, String nickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if(userRepository.existsByNickname(nickname) && !user.getNickname().equals(nickname)) {
            throw new BusinessException(ErrorCode.USER_NICKNAME_DUPLICATED);
        }

        user.updateNickname(nickname);
        return getMe(userId);
    }

}
