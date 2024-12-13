package com.backend.entity.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class ProfileImg {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long profileImgId;

    @Column(name = "profile_status")
    private int status; // 상태

    @Column(nullable = false)
    private String path; // 실제 저장된 파일 경로 (서버 디렉토리 또는 클라우드 URL)

    @Column(name = "user_id", nullable = false)
    private Long userId; // 프로필도

    @Column(name = "r_name")
    private String rName;

    @Column(name = "s_name")
    private String sName; // uuid 바뀐 파일이름

    @Column(name = "message")
    private String message; // 소개말

    @CreationTimestamp
    private LocalDateTime createdAt; // 파일 생성 날짜 및 시간

}
