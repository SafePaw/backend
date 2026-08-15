package com.ne7k.safepaw.notification.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushNotification;
import com.ne7k.safepaw.notification.domain.DeviceToken;
import com.ne7k.safepaw.notification.domain.NotificationType;
import com.ne7k.safepaw.notification.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final FcmPushSender fcmPushSender;
    private final DeviceTokenRepository deviceTokenRepository;

    public void sendToUser(Long userId, NotificationType type, String title, String body, Map<String, String> data) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUser_Id(userId);
        if (tokens.isEmpty()) {
            log.debug("[FCM] skip — no tokens userId={}", userId);
            return;
        }
        Map<String, String> payload = new LinkedHashMap<>(data);
        payload.put("type", type.name());

        for (DeviceToken dt : tokens) {
            try {
                Message msg = Message.builder()
                        .setToken(dt.getToken())
                        .putAllData(payload)
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .setWebpushConfig(WebpushConfig.builder()
                                .putHeader("Urgency", "high")
                                .setNotification(WebpushNotification.builder()
                                        .setTitle(title)
                                        .setBody(body)
                                        .build())
                                .build())
                        .build();
                fcmPushSender.send(msg);
            } catch (FirebaseMessagingException ex) {
                if (ex.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    deleteStaleToken(dt.getToken());
                    log.warn("[FCM] removed UNREGISTERED token userId={}", userId);
                } else {
                    log.error("[FCM] send failed userId={} code={} token=...{}",
                            userId, ex.getMessagingErrorCode(), tail(dt.getToken()), ex);
                }
            } catch (Exception ex) {
                log.error("[FCM] send failed userId={}", userId, ex);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteStaleToken(String token) {
        deviceTokenRepository.deleteByToken(token);
    }

    public void sendTerritoryIntrusion(IntrusionPushPayload p) {
        int percent = (int) Math.round(p.overlapRatio() * 100);
        String area = String.valueOf(Math.round(p.stolenAreaSquareMeters()));
        String body = buildIntrusionBody(p, percent, area);

        Map<String, String> data = new LinkedHashMap<>();
        data.put("intrusionId", String.valueOf(p.intrusionId()));
        data.put("victimTerritoryId", String.valueOf(p.victimTerritoryId()));
        data.put("victimDogId", String.valueOf(p.victimDogId()));
        data.put("victimDogName", nullToEmpty(p.victimDogName()));
        data.put("intruderDogId", String.valueOf(p.intruderDogId()));
        data.put("intruderDogName", nullToEmpty(p.intruderDogName()));
        data.put("overlapRatio", String.valueOf(p.overlapRatio()));
        data.put("overlapPercent", String.valueOf(percent));
        data.put("stolenAreaSquareMeters", area);
        data.put("victimStatusAfter", p.victimStatusAfter());
        data.put("remainderAreaSquareMeters", String.valueOf(Math.round(p.remainderAreaSquareMeters())));
        data.put("deepLink", "/intrusions/" + p.intrusionId());

        sendToUser(p.victimUserId(), NotificationType.TERRITORY_INTRUSION,
                "🚨 영토 침범 알림", body, data);
    }

    public void sendSeasonEndingSoon(Long userId, String seasonKey, int daysRemaining, String endedAtIso) {
        String body = daysRemaining == 1
                ? "내일 " + seasonKey + " 시즌이 끝나요. 랭킹을 확인해 보세요!"
                : seasonKey + " 시즌이 " + daysRemaining + "일 후 종료돼요. 마지막 스퍼트!";

        Map<String, String> data = new LinkedHashMap<>();
        data.put("seasonKey", seasonKey);
        data.put("daysRemaining", String.valueOf(daysRemaining));
        data.put("endedAt", endedAtIso);
        data.put("deepLink", "/ranking?season=" + seasonKey);

        sendToUser(userId, NotificationType.SEASON_ENDING_SOON, "⏳ 시즌 종료 임박", body, data);
    }

    private static String buildIntrusionBody(IntrusionPushPayload p, int percent, String area) {
        if ("CONQUERED".equals(p.victimStatusAfter())) {
            return p.intruderDogName() + "가 " + p.victimDogName() + " 영토를 완전히 점령했어요!";
        }
        if (p.overlapRatio() >= 0.5) {
            return p.intruderDogName() + "가 " + p.victimDogName()
                    + " 영토의 " + percent + "%를 침범했어요! (약 " + area + "㎡)";
        }
        return p.intruderDogName() + "가 " + p.victimDogName()
                + " 영토 일부(" + percent + "%, 약 " + area + "㎡)를 뺏었어요!";
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String tail(String token) {
        if (token == null || token.length() < 6) return "????";
        return token.substring(token.length() - 6);
    }

    public record IntrusionPushPayload(
            Long victimUserId,
            Long intrusionId,
            Long victimTerritoryId,
            Long victimDogId,
            String victimDogName,
            Long intruderDogId,
            String intruderDogName,
            double overlapRatio,
            double stolenAreaSquareMeters,
            double remainderAreaSquareMeters,
            String victimStatusAfter
    ) {}
}