package com.ne7k.safepaw.dog.dto.response;

public record MarkerUploadUrlResponse(
        String uploadUrl,
        String storageKey,
        int expiresIn // 초
) {
}
