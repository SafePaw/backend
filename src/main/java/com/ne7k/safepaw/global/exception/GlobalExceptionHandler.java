package com.ne7k.safepaw.global.exception;

import com.ne7k.safepaw.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice // error 발생시 이 파일로 전송
@Slf4j // sout 대체 logger
public class GlobalExceptionHandler {

    // 비즈니스 예외
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> BusinessExceptionHandler(BusinessException e) { // data null로 보내기에 void
        ErrorCode code = e.getErrorCode();
        return ResponseEntity
                .status(code.getStatus())
                .body(ApiResponse.fail(code, e.getMessage()));
    }

    // 프론트 사용자 입력값 예외
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream() // 입력값들 전부 가져오기
                .map(fe -> "%s : %s".formatted(fe.getField(), fe.getDefaultMessage())) // map 자료 변환
                .collect(Collectors.joining(", "));
        // [ex] email: 공백일 수 없습니다, age: 0살 이상이어야 합니다."
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.COMMON_INVALID_REQUEST, message)); // api response에 message 전송
    }

    // 기타 예외 - 예상 못한 에러들
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexcepted(Exception e) {
        log.error("예상 못한 에러", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR) // 서버 에러
                .body(ApiResponse.fail(ErrorCode.COMMON_INTERNAL_ERROR, "예상치 못한 오류가 발생했습니다"));
    }
}
