package com.ne7k.safepaw.crew.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CrewJoinRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9]{8}$") String inviteCode
) {}
