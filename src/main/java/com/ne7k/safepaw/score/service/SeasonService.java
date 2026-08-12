package com.ne7k.safepaw.score.service;

import com.ne7k.safepaw.score.domain.Season;
import com.ne7k.safepaw.score.repository.SeasonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class SeasonService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final SeasonRepository seasonRepository;

    /**
     * 현재 분기 시즌 get-or-create.
     * key: YYYY-Qn (Q1=1~3월 … Q4=10~12월), 구간 3개월.
     */
    @Transactional
    public Season currentSeason() {
        OffsetDateTime now = OffsetDateTime.now(KST);
        int month = now.getMonthValue();
        int q = (month - 1) / 3 + 1;
        String key = now.getYear() + "-Q" + q;

        return seasonRepository.findById(key).orElseGet(() -> {
            int startMonth = (q - 1) * 3 + 1;
            OffsetDateTime start = LocalDate.of(now.getYear(), startMonth, 1)
                    .atStartOfDay(KST)
                    .toOffsetDateTime();
            OffsetDateTime end = start.plusMonths(3).minusSeconds(1);
            return seasonRepository.save(Season.of(key, start, end));
        });
    }
}