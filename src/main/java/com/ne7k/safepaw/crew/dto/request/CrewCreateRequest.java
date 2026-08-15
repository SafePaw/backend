package com.ne7k.safepaw.crew.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrewCreateRequest(
        @NotBlank @Size(min = 2, max = 20) String name,
        @NotBlank @Pattern(regexp = "^#?[0-9A-Fa-f]{6}$") String territoryColor,
        @Size(max = 500) String imageKey
) {}
