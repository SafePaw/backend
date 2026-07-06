package com.ne7k.safepaw.walk.dto.request;

import jakarta.validation.Valid;
import java.util.List;

public record WalkFinishRequest(
        @Valid List<WalkPointDto> lastPoints   // null/빈 배열 허용
) {}
