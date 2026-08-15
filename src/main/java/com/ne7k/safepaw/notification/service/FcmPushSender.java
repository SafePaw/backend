package com.ne7k.safepaw.notification.service;

import com.google.firebase.messaging.Message;

public interface FcmPushSender {
    String send(Message message) throws Exception;
}