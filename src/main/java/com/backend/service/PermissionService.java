package com.backend.service;


import com.backend.document.drive.DrivePermission;
import com.backend.document.page.PagePermission;
import com.backend.entity.folder.Permission;
import com.backend.entity.user.User;
import com.backend.repository.PermissionRepository;
import com.backend.repository.UserRepository;
import com.backend.repository.drive.DrivePermissionRepository;
import com.backend.repository.drive.PermissionMySQLRepository;
import com.backend.util.PermissionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/*
    날짜 : 2024.12.05
    이름 : 하진희
    내용: 퍼미션 enum 에 사용할 퍼미션 서비스

    READ 1
    WRITE 2
    FULL 4 => read,write,delete,share 가능 (페이지삭제권한) -> owner 유저가 생성시 full\
    share 8

    READ + WRITE: 0001 | 0010 = 0011 (3)
    READ + SHARE: 0001 | 1000 = 1001 (9)
    READ + WRITE + SHARE: 0001 | 0010 | 1000 = 1011 (11)
 */

@Service
@RequiredArgsConstructor
@Log4j2
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final DrivePermissionRepository drivePermissionRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final PermissionMySQLRepository permissionMySQLRepository;


    // 권한 확인
    public boolean hasPermission(String pageId, PermissionType permissionType,String type) {
        Optional<PagePermission> pagePermission = permissionRepository.findByPageId(pageId);
        return pagePermission.map(permission -> PermissionType.hasPermission(permission.getPermission(), permissionType))
                .orElse(false);
    }

    // 권한 추가
    public Permission addPermission(String id, String userId, String type, int permissions) {
        if (permissions == 0){
            throw new IllegalArgumentException("PermissionTypes cannot be null or empty");
        }
        if (type.equals("page")) {
            PagePermission pagePermission = permissionRepository.findByPageIdAndUserId(id, userId)
                    .orElse(PagePermission.builder()
                            .pageId(id)
                            .userId(userId)
                            .permission(permissions) // 기본 권한 없음
                            .build());
            permissionRepository.save(pagePermission);
        } else if (type.equals("folder")) {
            DrivePermission drivePermission = drivePermissionRepository.findByFolderIdAndUserId(id, userId)
                    .orElse(DrivePermission.builder()
                            .folderId(id)
                            .userId(userId)
                            .permission(permissions) // 기본 권한 없음
                            .build());
            drivePermissionRepository.save(drivePermission);
        } else if (type.equals("file")) {
            DrivePermission drivePermission = drivePermissionRepository.findByFileIdAndUserId(id, userId)
                    .orElse(DrivePermission.builder()
                            .fileId(id)
                            .userId(userId)
                            .permission(permissions) // 기본 권한 없음
                            .build());
            drivePermissionRepository.save(drivePermission);
        }
        return null;
    }



    //mysql 권한 저장
    public Permission savePermission(String id,User user, String type, String permissionType) {
        Permission permission = Permission.builder()
                .type(type)
                .permissions(permissionType)
                .user(user)
                .typeId(id)
                .updatedAt(LocalDateTime.now())
                .build();
        Permission savedPermission = permissionMySQLRepository.save(permission);

        return savedPermission;
    }



        // 권한 제거
    public void removePermission(String id, PermissionType permissionType,String type) {

        if(type.equals("page")){
            PagePermission pagePermission = permissionRepository.findByPageId(id)
                    .orElseThrow(() -> new RuntimeException("PagePermission not found"));

            int updatedPermission = PermissionType.removePermission(pagePermission.getPermission(), permissionType);
            pagePermission.updatePagePermission(updatedPermission);
            permissionRepository.save(pagePermission);
        }else if(type.equals("folder")){
            DrivePermission drivePermission = drivePermissionRepository.findByFolderId(id)
                    .orElseThrow(() -> new RuntimeException("DrivePermission not found"));
            int updatedPermission = PermissionType.removePermission(drivePermission.getPermission(), permissionType);
            drivePermission.updatePermission(updatedPermission);
            drivePermissionRepository.save(drivePermission);

        }else if(type.equals("file")){
            DrivePermission drivePermission = drivePermissionRepository.findByFileId(id)
                    .orElseThrow(() -> new RuntimeException("DrivePermission not found"));
            int updatedPermission = PermissionType.removePermission(drivePermission.getPermission(), permissionType);
            drivePermission.updatePermission(updatedPermission);
            drivePermissionRepository.save(drivePermission);
        }
    }


}
