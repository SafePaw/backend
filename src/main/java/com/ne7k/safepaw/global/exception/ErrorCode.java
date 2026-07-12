package com.ne7k.safepaw.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // -------- 공통 (COMMON_*) --------
    COMMON_INVALID_REQUEST    (HttpStatus.BAD_REQUEST,           "요청 형식이 올바르지 않습니다."),
    COMMON_UNAUTHORIZED       (HttpStatus.UNAUTHORIZED,          "인증이 필요합니다."),
    COMMON_FORBIDDEN          (HttpStatus.FORBIDDEN,             "권한이 없습니다."),
    COMMON_INTERNAL_ERROR     (HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),
    COMMON_NOT_FOUND          (HttpStatus.NOT_FOUND,             "리소스를 찾을 수 없습니다."),

    // -------- 인증 (AUTH_*) --------
    AUTH_EMAIL_DUPLICATED     (HttpStatus.CONFLICT,           "이미 가입된 이메일입니다."),
    AUTH_INVALID_CREDENTIALS  (HttpStatus.UNAUTHORIZED,       "이메일 또는 비밀번호가 올바르지 않습니다."),
    AUTH_WEAK_PASSWORD        (HttpStatus.BAD_REQUEST,        "비밀번호 규칙을 만족하지 않습니다."),
    AUTH_UNSUPPORTED_PROVIDER (HttpStatus.BAD_REQUEST,        "지원하지 않는 소셜 로그인 공급자입니다."),
    AUTH_INVALID_ID_TOKEN     (HttpStatus.UNAUTHORIZED,       "소셜 로그인 토큰이 유효하지 않습니다."),
    AUTH_KAKAO_API_FAILED     (HttpStatus.BAD_GATEWAY,        "카카오 API 호출에 실패했습니다."),
    AUTH_NAVER_API_FAILED     (HttpStatus.BAD_GATEWAY,        "네이버 API 호출에 실패했습니다."),
    AUTH_GOOGLE_API_FAILED    (HttpStatus.BAD_GATEWAY,        "구글 토큰 검증에 실패했습니다."),
    AUTH_EXPIRED_REFRESH      (HttpStatus.UNAUTHORIZED,       "Refresh 토큰이 만료되었습니다."),
    AUTH_REUSED_REFRESH       (HttpStatus.UNAUTHORIZED,       "Refresh 토큰 재사용이 감지되었습니다. 다시 로그인 해주세요."),

    // -------- 사용자 (USER_*) --------
    USER_NOT_FOUND            (HttpStatus.NOT_FOUND,          "사용자를 찾을 수 없습니다."),
    USER_NICKNAME_DUPLICATED  (HttpStatus.CONFLICT,           "이미 사용 중인 닉네임입니다."),

    // -------- 강아지 (DOG_*) --------
    DOG_NOT_FOUND             (HttpStatus.NOT_FOUND,          "강아지를 찾을 수 없습니다."),
    DOG_NOT_OWNED             (HttpStatus.FORBIDDEN,          "본인의 강아지가 아닙니다."),
    DOG_LIMIT_EXCEEDED        (HttpStatus.UNPROCESSABLE_ENTITY, "강아지 등록 가능 수를 초과했습니다."),

    // -------- 산책 (WALK_*) --------
    WALK_NOT_FOUND            (HttpStatus.NOT_FOUND,          "산책 세션을 찾을 수 없습니다."),
    WALK_ALREADY_FINISHED     (HttpStatus.CONFLICT,           "이미 종료된 산책입니다."),
    WALK_NOT_ONGOING          (HttpStatus.CONFLICT,           "진행 중인 산책이 아닙니다."),
    WALK_TOO_SHORT            (HttpStatus.UNPROCESSABLE_ENTITY, "산책 시간이 5분 미만입니다."),
    WALK_ONGOING_EXISTS       (HttpStatus.CONFLICT,           "이미 진행 중인 산책이 있습니다."),
    WALK_INVALID_POINT_BATCH  (HttpStatus.UNPROCESSABLE_ENTITY, "GPS 배치 형식이 올바르지 않습니다."),
    WALK_ALREADY_PAUSED   (HttpStatus.CONFLICT,                 "이미 일시정지 중인 산책입니다."),
    WALK_NOT_PAUSED       (HttpStatus.CONFLICT,                 "일시정지 상태가 아닙니다."),

    // -------- 영토 (TERRITORY_*) --------
    TERRITORY_NOT_FOUND       (HttpStatus.NOT_FOUND,          "영토를 찾을 수 없습니다."),
    TERRITORY_LOOP_NOT_CLOSED (HttpStatus.UNPROCESSABLE_ENTITY, "루프가 닫히지 않았습니다."),
    TERRITORY_TOO_SMALL       (HttpStatus.UNPROCESSABLE_ENTITY, "영토 면적이 너무 작습니다."),
    TERRITORY_TOO_NARROW      (HttpStatus.UNPROCESSABLE_ENTITY, "루프 폭이 15m 미만입니다."),
    TERRITORY_INSUFFICIENT_POINTS (HttpStatus.UNPROCESSABLE_ENTITY, "유효 GPS 포인트가 부족합니다."),
    TERRITORY_DUPLICATE       (HttpStatus.UNPROCESSABLE_ENTITY, "24시간 내 동일 영역이 이미 인정되었습니다."),
    TERRITORY_BBOX_TOO_LARGE  (HttpStatus.UNPROCESSABLE_ENTITY, "조회 범위가 너무 큽니다."),

    // -------- 푸시 (NOTIFY_*) --------
    NOTIFY_DEVICE_TOKEN_INVALID (HttpStatus.UNPROCESSABLE_ENTITY, "FCM 토큰이 유효하지 않습니다."),

    // -------- 시즌 / 랭킹 (SEASON_*) --------
    SEASON_NOT_FOUND          (HttpStatus.NOT_FOUND,          "시즌을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String defaultMessage;
    // ... constructor, getters

}
