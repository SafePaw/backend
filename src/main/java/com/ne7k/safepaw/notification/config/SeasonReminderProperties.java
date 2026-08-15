package com.ne7k.safepaw.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "safepaw.notification.season-reminder")
public record SeasonReminderProperties(
        boolean enabled,
        String cron,
        String zone
) {}