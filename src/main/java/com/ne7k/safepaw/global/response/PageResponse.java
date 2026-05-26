package com.ne7k.safepaw.global.response;

import org.springframework.data.domain.Page;

import java.util.List;

// 랭킹, 산책 기록 등 Page Response
public record PageResponse<T>(
        List<T> content,   // 실제 데이터 목록
        int page,   // 현재 페이지
        int size,   // 한 페이지 데이터 개수
        long totalElements, // db에 존재하는 총 데이터 개수
        int totalPages, // 전체 페이지수
        boolean hasNext // 다음 페이지 여부
) {
    // DB에서 받아온 데이터 
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
