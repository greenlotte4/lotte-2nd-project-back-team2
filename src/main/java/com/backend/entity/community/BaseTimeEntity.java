package com.backend.entity.community;

/*
    날짜 : 2024/12/03
    이름 : 박서홍
    내용 : BaseTime Entity 작성
 */

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@EntityListeners(AuditingEntityListener.class) // Auditing 활성화
@MappedSuperclass
public abstract class BaseTimeEntity {
    @CreatedDate // 엔티티 생성 시 자동으로 현재 시간 저장
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate // 엔티티 수정 시 자동으로 현재 시간 저장
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}


