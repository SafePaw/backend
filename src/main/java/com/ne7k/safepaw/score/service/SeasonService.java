package com.ne7k.safepaw.score.service;

import com.ne7k.safepaw.score.domain.Season;
import com.ne7k.safepaw.score.repository.SeasonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class SeasonService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final SeasonRepository seasonRepository;

    /** 'YYYY-MM' 현재 시즌 — 없으면 생성 (get-or-create) */
    @Transactional
    public Season currentSeason() {
        OffsetDateTime now = OffsetDateTime.now(KST);
        String key = now.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return seasonRepository.findById(key).orElseGet(() -> {
            OffsetDateTime start = now.withDayOfMonth(1).toLocalDate().atStartOfDay(KST).toOffsetDateTime();
            OffsetDateTime end = start.plusMonths(1).minusSeconds(1);
            return seasonRepository.save(Season.of(key, start, end));
        });
    }
}