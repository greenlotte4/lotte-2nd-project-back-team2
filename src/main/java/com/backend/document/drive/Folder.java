package com.backend.document.drive;

import com.backend.dto.request.drive.ShareDept;
import com.backend.dto.request.drive.SharedUser;
import com.backend.dto.response.drive.FolderDto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Document(collection = "folders")
public class Folder {

    @Id
    private String id; // 폴더의 고유 ID
    private String parentId; // 상위 폴더 ID, 최상위 폴더는 null
    private String path; // 폴더 경로
    private String folderUUID;
    private String name; // 폴더 이름
    private String ownerId;
    private String type; //DRIVE, ROOT, FOLDER
    private Double order; // 폴더 순서 (같은 부모 폴더 내에서의 정렬)
    private String description; // 폴더 설명
    @CreationTimestamp
    private LocalDateTime createdAt; // 폴더 생성 날짜 및 시간
    @LastModifiedDate
    private LocalDateTime updatedAt; // 폴더 수정 날짜 및 시간
    private List<Folder> children = new ArrayList<>(); // 하위 폴더 목록
    private List<FileMogo> files = new ArrayList<>(); // 폴더 내 파일 목록
    @Builder.Default
    private int isShared =0 ; // 공유 여부  0: 나만사용, 2: 선택한 구성원 3: 전체구성원
    @Builder.Default
    private int linkSharing =0 ; //링크 공유 여부  허용하지않음 0 , 허용 1
    @Builder.Default
    private int status = 1; // 상태

    @Builder.Default
    private String sharedUser = "[]";
    @Builder.Default
    private String sharedDept = "[]";



    @Setter
    @Builder.Default
    private List<SharedUser> sharedUsers = new ArrayList<>(); // 공유 사용자

    @Setter
    @Builder.Default
    private List<ShareDept> sharedDepts = new ArrayList<>(); // 공유 부서

    @Setter
    @Builder.Default
    private List<Invitation> invitations = new ArrayList<>();

    @Builder.Default
    private int target=0;

    @Builder.Default
    private int isPinned = 0; // 1: 고정 폴더

    public FolderDto toDTO() {
        return FolderDto.builder()
                .id(this.id)
                .parentId(this.parentId)
                .path(this.path)
                .name(this.name)
                .folderUUID(this.folderUUID)
                .ownerId(this.ownerId)
                .order(this.order)
                .type(this.type)
                .description(this.description)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .isShared(this.isShared)
                .sharedUser(this.sharedUser)
                .sharedDept(this.sharedDept)
                .linkSharing(this.linkSharing)
                .status(this.status)
                .isPinned(this.isPinned)
                .shareDepts(this.sharedDepts)
                .sharedUsers(this.sharedUsers)
                .invitations(this.invitations)
                .target(this.target)
                .build();
    }

    public void newFolderName(String newfolderName){
        this.name = newfolderName;
    }

    public void moveOrder(double newOrder){this.order = newOrder;}

    public void updateSharedUser(String newSharedUser){this.sharedUser = newSharedUser;}

    public void updateSharedDept(String newSharedDept){this.sharedDept = newSharedDept;}
    public void updateSharedUsers(List<SharedUser> newSharedUsers){this.sharedUsers = newSharedUsers;}
    public void setTarget(){this.target=1;}

}


