package com.backend.document.drive;


import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter

@Document(collection = "invitation")
public class Invitation {
    @Id
    private String id;                  // 초대 고유 ID
    private String email;               // 초대된 사용자 이메일
    private String type;
    private String sharedId;            // 공유된 폴더 또는 파일 ID
    @Builder.Default
    private String status="PENDING";              // 초대 상태 (PENDING, ACCEPTED, DECLINED, CANCELLED,EXPIRED)
    private String permission;          // 권한 (READ, WRITE, FULL)

    @CreatedDate
    private LocalDateTime createdAt;    // 초대 생성 시간
    @LastModifiedDate
    private LocalDateTime updatedAt;    // 초대 수정 시간

    private LocalDateTime expiredAt;    // 초대 만료 시간 (옵션)

    @PrePersist
    public void onExpiredAT() {
        if (expiredAt == null) { // 만료일이 설정되어 있지 않은 경우에만 설정
            expiredAt = LocalDateTime.now().plusDays(2); // 현재 시간으로부터 2일 후
        }
    }

    private void update(String id,LocalDateTime expiredAt,LocalDateTime createdAt){
        this.expiredAt = expiredAt;
        this.createdAt = createdAt;
        this.sharedId= id;
    }

    public void setState(String state){
        this.status=state;
    }



}
