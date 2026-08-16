package com.ne7k.safepaw.notification.service;

import com.google.firebase.messaging.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "safepaw.fcm.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingFcmPushSender implements FcmPushSender {

    @Override
    public String send(Message message) {
        log.info("[FCM-STUB] skip real send (safepaw.fcm.enabled=false)");
        return "stub-message-id";
    }
}