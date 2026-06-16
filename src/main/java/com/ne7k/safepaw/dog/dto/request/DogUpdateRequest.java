package com.ne7k.safepaw.dog.dto.request;

import com.ne7k.safepaw.dog.domain.Gender;
import com.ne7k.safepaw.dog.domain.MarkerImageType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record DogUpdateRequest(
        @Size(min = 1, max = 20) String name,
        Gender gender,
        @Size(max = 50) String breed,
        @Min(0) @Max(30) Integer age,
        @DecimalMin("0.1") @DecimalMax("200.0") BigDecimal weightKg,
        @Pattern(regexp = "^#?[0-9A-Fa-f]{6}$") String territoryColor,
        MarkerImageType markerImageType,
        @Size(max = 500) String markerImageValue
) {
}
