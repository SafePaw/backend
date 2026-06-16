package com.ne7k.safepaw.dog.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MarkerUploadUrlRequest(
        @NotBlank String contentType
) {
}
