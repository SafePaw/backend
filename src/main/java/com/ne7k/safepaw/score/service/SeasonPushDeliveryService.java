package com.ne7k.safepaw.score.service;

import com.ne7k.safepaw.score.domain.SeasonPushDelivery;
import com.ne7k.safepaw.score.repository.SeasonPushDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeasonPushDeliveryService {

    private final SeasonPushDeliveryRepository deliveryRepository;

    @Transactional
    public boolean tryReserve(Long userId, String seasonKey, String reminderKind) {
        if (deliveryRepository.existsByUserIdAndSeasonKeyAndReminderKind(userId, seasonKey, reminderKind)) {
            return false;
        }
        try {
            deliveryRepository.saveAndFlush(SeasonPushDelivery.record(userId, seasonKey, reminderKind));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    @Transactional
    public void release(Long userId, String seasonKey, String reminderKind) {
        deliveryRepository.deleteByUserIdAndSeasonKeyAndReminderKind(userId, seasonKey, reminderKind);
    }
}