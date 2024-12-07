package com.backend.dto.response.drive;

import com.backend.entity.folder.File;
import com.backend.document.drive.Folder;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
@Builder
public class FolderDto {

    private String id; // 폴더의 고유 ID
    private Folder parent; // 상위 폴더 (최상위 폴더의 경우 NULL)
    private String name; // 폴더 이름
    private String folderUUID;
    private String ownerId; // 폴더를 소유한 사용자
    private Double order; // 폴더 순서 (같은 부모 폴더 내에서의 정렬)
    private String description; // 폴더 설명
    private String type;
    private LocalDateTime createdAt; // 폴더 생성 날짜 및 시간
    private LocalDateTime updatedAt; // 폴더 수정 날짜 및 시간
    private List<Folder> children = new ArrayList<>(); // 하위 폴더 목록
    private List<File> files = new ArrayList<>(); // 폴더 내 파일 목록
    private int isShared =0 ; // 공유 여부  0: 나만사용, 2: 선택한 구성원 3: 전체구성원
    private int linkSharing =0 ; //링크 공유 여부  허용하지않음 0 , 허용 1
    private int status = 0; // 상태
    private int isPinned=0;
    private String parentId;
    private String path;
    private int size;
    private String parentPath;
}
