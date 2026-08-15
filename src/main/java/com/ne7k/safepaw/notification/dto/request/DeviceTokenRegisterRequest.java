package com.ne7k.safepaw.notification.dto.request;

import com.ne7k.safepaw.notification.domain.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeviceTokenRegisterRequest(
        @NotNull DevicePlatform platform,
        @NotBlank @Size(max = 512) String token
) {}