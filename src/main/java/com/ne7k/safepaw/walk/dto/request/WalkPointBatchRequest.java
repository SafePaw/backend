package com.ne7k.safepaw.walk.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record WalkPointBatchRequest(
        @NotEmpty @Size(max = 60) @Valid List<WalkPointDto> points
) {}