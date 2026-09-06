package com.ne7k.safepaw.walk.dto.response;

public record ShareCardUploadUrlResponse(
        String uploadUrl,
        String imageKey,
        int expiresInSeconds
) {}
