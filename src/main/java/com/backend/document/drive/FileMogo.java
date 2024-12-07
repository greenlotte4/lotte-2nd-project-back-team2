package com.backend.document.drive;

import com.backend.dto.request.FileRequestDto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Builder
@Document(collection = "files")
public class FileMogo {

    @Id
    private String id; // 파일의 고유 ID

    private String folderId; // 파일이 속한 폴더

    private String originalName;  //파일 원래 이름 (사용자가 지정한 이름)

    private String savedName; // 파일 저장 이름

    private String path; // 실제 저장된 파일 경로 (서버 디렉토리 또는 클라우드 URL)

    private Double file_order; // 파일 순서 (같은 폴더 내에서의 정렬)

    private Long size; // 파일 크기 (바이트 단위)

    @Field("owner_uid")
    private String ownerUid; // 파일을 업로드한 사용자

    private Integer version = 1; // 파일 버전 (업데이트 시 증가)

    private int isShared = 0; // 1이 share중
    private int isPinned = 0; // 1: 고정 폴더

    private int status =0 ; // 파일 삭제 여부 (논리적 삭제)

    @CreationTimestamp
    private LocalDateTime createdAt; // 파일 생성 날짜 및 시간

    @LastModifiedDate
    private LocalDateTime updatedAt; // 파일 수정 날짜 및 시간

    public FileRequestDto toDto(){
        FileRequestDto fileRequestDto = FileRequestDto.builder()
                .id(this.id)
                .folderId(this.folderId)
                .originalName(this.originalName)
                .savedName(this.savedName)
                .path(this.path)
                .file_order(this.file_order)
                .size(this.size)
                .ownerUid(this.ownerUid)
                .version(this.version)
                .status(this.status)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .size(this.size)
                .folderId(this.folderId)
                .build();
        return fileRequestDto;
    }
}
