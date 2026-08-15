package com.ne7k.safepaw.notification.service;

import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.notification.domain.DeviceToken;
import com.ne7k.safepaw.notification.dto.request.DeviceTokenRegisterRequest;
import com.ne7k.safepaw.notification.repository.DeviceTokenRepository;
import com.ne7k.safepaw.user.domain.User;
import com.ne7k.safepaw.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void register(Long userId, DeviceTokenRegisterRequest req) {
        String token = req.token().trim();
        if (token.length() < 20) {
            throw new BusinessException(ErrorCode.NOTIFY_DEVICE_TOKEN_INVALID);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        deviceTokenRepository.findByToken(token)
                .ifPresentOrElse(
                        existing -> existing.refreshOwnership(user, req.platform()),
                        () -> deviceTokenRepository.save(DeviceToken.of(user, req.platform(), token))
                );
    }

    @Transactional
    public void unregister(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.NOTIFY_DEVICE_TOKEN_INVALID);
        }
        deviceTokenRepository.deleteByToken(token.trim());
    }

    @Transactional
    public void unregisterAll(Long userId) {
        deviceTokenRepository.deleteByUser_Id(userId);
    }
}