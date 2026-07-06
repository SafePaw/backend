package com.ne7k.safepaw.notification.listener;

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

    // private final PushNotificationService push;  // FCM — set5 에서 실제 구현

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIntrusion(TerritoryIntrusionEvent e) {
        // 커밋 후 비동기. FCM 발송이 도메인 트랜잭션을 롤백시키지 않도록 분리 (02-backend §2.9)
        log.info("[INTRUSION] victimUser={} victimTerritory={} by dog={} overlap={}",
                e.victimUserId(), e.victimTerritoryId(), e.intruderDogName(), e.overlapRatio());
        // push.send(...); // set5
    }
}