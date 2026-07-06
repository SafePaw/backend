package com.ne7k.safepaw.walk.dto.request;

import jakarta.validation.constraints.NotNull;

public record WalkStartRequest(@NotNull Long dogId) {

}