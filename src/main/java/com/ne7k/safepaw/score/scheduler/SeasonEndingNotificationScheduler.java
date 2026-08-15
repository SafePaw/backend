package com.ne7k.safepaw.score.scheduler;

import com.ne7k.safepaw.notification.repository.DeviceTokenRepository;
import com.ne7k.safepaw.notification.service.PushNotificationService;
import com.ne7k.safepaw.score.domain.Season;
import com.ne7k.safepaw.score.service.SeasonPushDeliveryService;
import com.ne7k.safepaw.score.service.SeasonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "safepaw.notification.season-reminder.enabled", havingValue = "true", matchIfMissing = true)
public class SeasonEndingNotificationScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final SeasonService seasonService;
    private final DeviceTokenRepository deviceTokenRepository;
    private final SeasonPushDeliveryService deliveryService;
    private final PushNotificationService push;

    @Scheduled(cron = "${safepaw.notification.season-reminder.cron:0 0 9 * * *}", zone = "Asia/Seoul")
    public void sendSeasonEndingReminders() {
        Season season = seasonService.currentSeason();
        LocalDate today = LocalDate.now(KST);
        LocalDate endDate = season.getEndedAt().atZoneSameInstant(KST).toLocalDate();
        long daysRemaining = ChronoUnit.DAYS.between(today, endDate);

        Integer reminderDays = switch ((int) daysRemaining) {
            case 7 -> 7;
            case 1 -> 1;
            default -> null;
        };
        if (reminderDays == null) {
            log.debug("[SEASON-FCM] skip season={} daysRemaining={}", season.getKey(), daysRemaining);
            return;
        }

        String reminderKind = reminderDays == 7 ? "D7" : "D1";
        String endedAtIso = season.getEndedAt().toString();
        List<Long> userIds = deviceTokenRepository.findDistinctUserIdsWithDogs();

        int sent = 0;
        for (Long userId : userIds) {
            if (!deliveryService.tryReserve(userId, season.getKey(), reminderKind)) {
                continue;
            }
            try {
                push.sendSeasonEndingSoon(userId, season.getKey(), reminderDays, endedAtIso);
                sent++;
            } catch (Exception ex) {
                deliveryService.release(userId, season.getKey(), reminderKind);
                log.error("[SEASON-FCM] send failed userId={} season={}", userId, season.getKey(), ex);
            }
        }
        log.info("[SEASON-FCM] season={} kind={} candidates={} sent={}",
                season.getKey(), reminderKind, userIds.size(), sent);
    }
}