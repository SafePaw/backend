package com.ne7k.safepaw.global.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

// 강아지, 유저, 산책 기록 등은 전부 언제 만들어졌는지 언제 수정되었는지 필요하기에 대체

@Getter
@MappedSuperclass // db로 제작하는 것은 아니고 자식들이 db로 제작되면 물려받음
@EntityListeners(AuditingEntityListener.class) // 시간 자동 작성
public abstract class BaseTimeEntity { // 추상 클래스로 객체 생성 방지

    // 생성일
    @CreatedDate
    @Column(nullable = false, updatable = false) // 수정 불가
    private LocalDateTime createdAt;

    // 수정일
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

}
