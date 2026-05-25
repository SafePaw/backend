package com.ne7k.safepaw.global.exception;

import lombok.Getter;

// 예상된 버그와 실제 버그를 구분하기 위함
@Getter
public class BusinessException extends RuntimeException {
    // runtime excetpion을 사용하는 이유는 throw 사용하지 않기 위함

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorcode) {
        super(errorcode.getDefaultMessage()); // 부모 전달
        this.errorCode = errorcode; // globalexceptionhandler
    }

    // 메시지 포함 버전
    public BusinessException(ErrorCode errorcode, String message) {
        super(message);
        this.errorCode = errorcode;
    }
}
