package com.backend.dto.request.drive;


import com.backend.dto.response.drive.FolderDto;
import com.backend.util.PermissionType;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class NewDriveRequest {
    private String name;         // 드라이브 이름
    private String owner;        // 소유자 이름
    private String description;  // 설명
    private double order;           // 순서
    private String type;
    private String driveMaster;  // 드라이브 마스터 이름
    private String masterEmail;  // 드라이브 마스터 이메일
    private List<String> shareUsers; // 공유 사용자 목록
    private int isShared;        // 공유 여부 (0: 나만 사용, 1: 선택한 구성원, 2: 전체 구성원)
    private int linkSharing;  // 링크 공유 (0: 허용안함, 1: 허용함)
    private String parentId;
    private int status;
    private int permissions;

    private List<ShareDept> shareDepts = new ArrayList<>();
    private List<SharedUser> sharedUsers = new ArrayList<>();

    private FolderDto parentFolder;
}
