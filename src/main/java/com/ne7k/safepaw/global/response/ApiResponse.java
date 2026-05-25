package com.ne7k.safepaw.global.response;

import com.ne7k.safepaw.global.exception.ErrorCode;

// 성공 여부 및 데이터 매개변수, 에러 바디
public record ApiResponse<T>(boolean success, T data, ErrorBody error) {

    // Error Body (code, message)
    public record ErrorBody(String code, String message) {}

    // Success Response
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    // Fail Response (Error code getter로 가져옴, message는 나중에 커스텀용)
    public static <T> ApiResponse<T> fail(ErrorCode code, String message) {
        return new ApiResponse<>(false, null, new ErrorBody(code.name(), message));
    }

}
