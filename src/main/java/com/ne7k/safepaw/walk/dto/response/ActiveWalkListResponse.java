package com.ne7k.safepaw.walk.dto.response;

import java.util.List;

public record ActiveWalkListResponse(
        List<ActiveWalkResponse> walks
) {
    public static ActiveWalkListResponse of(List<ActiveWalkResponse> walks) {
        return new ActiveWalkListResponse(walks);
    }
}
