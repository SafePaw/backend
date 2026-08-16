package com.ne7k.safepaw.crew.dto.request;

import jakarta.validation.constraints.NotNull;

public record CrewTransferRequest(
        @NotNull Long targetUserId
) {}
