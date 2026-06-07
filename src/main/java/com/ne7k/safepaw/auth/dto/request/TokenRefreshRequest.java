package com.ne7k.safepaw.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(@NotBlank String refreshToken) {
}
