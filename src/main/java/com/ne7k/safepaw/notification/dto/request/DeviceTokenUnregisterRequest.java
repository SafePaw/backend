package com.ne7k.safepaw.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceTokenUnregisterRequest(
        @NotBlank @Size(max = 512) String token
) {}