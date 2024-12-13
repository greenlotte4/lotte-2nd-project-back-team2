package com.backend.dto.request.user;

import com.backend.document.drive.FileMogo;
import com.backend.entity.user.ProfileImg;
import com.backend.entity.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReqProfileDTO {
    private Long profileImgId;

    private int status; // 상태

    private String path; // 실제 저장된 파일 경로 (서버 디렉토리 또는 클라우드 URL)

    private Long userId; // 프로필도

    private String rName;

    private String sName; // uuid 바뀐 파일이름

    private String message; // 소개말

    private LocalDateTime createdAt; // 파일 생성 날짜 및 시간

    public ProfileImg toEntity(){
        ProfileImg entity = ProfileImg.builder()
                .profileImgId(profileImgId)
                .status(status)
                .path(path)
                .userId(userId)
                .rName(rName)
                .sName(sName)
                .message(message)
                .createdAt(createdAt)
                .build();
        return entity;
    }
}
