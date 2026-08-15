package com.ne7k.safepaw.crew.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "safepaw.crew")
public record CrewProperties(
        int maxMembers,
        int inviteCodeLength
) {}
