package com.ne7k.safepaw.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "safepaw.fcm.enabled", havingValue = "true")
@RequiredArgsConstructor
public class FirebaseFcmPushSender implements FcmPushSender {

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public String send(Message message) throws Exception {
        return firebaseMessaging.send(message);
    }
}