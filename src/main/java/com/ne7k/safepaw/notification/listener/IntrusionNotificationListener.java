package com.ne7k.safepaw.notification.listener;

import com.ne7k.safepaw.notification.service.PushNotificationService;
import com.ne7k.safepaw.notification.service.PushNotificationService.IntrusionPushPayload;
import com.ne7k.safepaw.territory.event.TerritoryIntrusionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntrusionNotificationListener {

    private final PushNotificationService push;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIntrusion(TerritoryIntrusionEvent e) {
        log.info("[INTRUSION-FCM] victimUser={} intrusion={} overlap={}% status={}",
                e.victimUserId(), e.intrusionId(),
                Math.round(e.overlapRatio() * 100), e.victimStatusAfter());

        push.sendTerritoryIntrusion(new IntrusionPushPayload(
                e.victimUserId(),
                e.intrusionId(),
                e.victimTerritoryId(),
                e.victimDogId(),
                e.victimDogName(),
                e.intruderDogId(),
                e.intruderDogName(),
                e.overlapRatio(),
                e.stolenAreaSquareMeters(),
                e.remainderAreaSquareMeters(),
                e.victimStatusAfter()
        ));
    }
}