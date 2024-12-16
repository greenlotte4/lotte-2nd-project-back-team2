package com.backend.service;


import com.backend.document.drive.FileMogo;
import com.backend.document.drive.Folder;
import com.backend.dto.request.drive.*;
import com.backend.entity.group.GroupMapper;
import com.backend.entity.user.User;
import com.backend.repository.GroupMapperRepository;
import com.backend.repository.UserRepository;
import com.backend.repository.drive.FileMogoRepository;
import com.backend.repository.drive.FolderMogoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Log4j2
public class ShareService {


    private final PermissionService permissionService;
    private final GroupMapperRepository groupMapperRepository;
    private final FolderMogoRepository folderMogoRepository;
    private final MongoTemplate mongoTemplate;
    private final FileMogoRepository fileMogoRepository;
    private final UserService userService;
    private final UserRepository userRepository;

    //department
    public boolean shareUser(ShareRequestDto shareRequestDto,String type,String id ) {

        if(shareRequestDto.getUserType().equals("department")){
            if(type.equals("folder")){
                Folder folder = folderMogoRepository.findById(id).orElseThrow(() -> new RuntimeException("Folder not found"));
                folder.setSharedUsers(shareRequestDto.getSharedUsers());
                folder.setTarget();
                folderMogoRepository.save(folder);

                return true;
            }else if(type.equals("type")){
                return true;

            }


        }else if(shareRequestDto.getUserType().equals("individual")){
            return true;

        }
        return false;



    }

    public boolean sharedDepartment(ShareRequestDto shareRequestDto ,String type, String id) {

        List<ShareDept> sharedDeptList=new ArrayList<>();
        List<SharedUser> sharedUserList=new ArrayList<>();
        if(shareRequestDto.getDepartments() != null){
            for(DepartmentDto group: shareRequestDto.getDepartments()){
                //mogo업데이트
                String permission = group.getPermission();
                ShareDept sharedDept = ShareDept.builder()
                        .cnt(group.getDepartmentCnt())
                        .deptId(group.getDepartmentId())
                        .deptName(group.getDepartmentName())
                        .permission(group.getPermission())
                        .build();
                sharedDeptList.add(sharedDept);

                List<GroupMapper> groupMapper = groupMapperRepository.findGroupMapperByGroup_Id(Long.valueOf(group.getDepartmentId()));

                if (groupMapper != null) {

                    for (GroupMapper gm : groupMapper) {
                        User user = gm.getUser();
                        SharedUser sharedUser = SharedUser.builder()
                                .id(user.getId())
                                .authority(user.getRole() != null ? user.getRole().toString() : "default") // 기본 권한 값 설정                            .email(user.getEmail())
                                .uid(user.getUid())
                                .permission(permission)
                                .name(user.getName())
                                .email(user.getEmail())
                                .group(group.getDepartmentName())
                                .profile(user.getProfileImgPath())
                                .build();
                        sharedUserList.add(sharedUser);
                    }
                }

            }
        }

        if(type.equals("folder")){
            Folder folder = folderMogoRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Folder ID or Type"));

            List<SharedUser> existingSharedUsers = folder.getSharedUsers();
            Set<SharedUser> uniqueSharedUsers = new HashSet<>(existingSharedUsers);
            uniqueSharedUsers.addAll(sharedUserList); // 새로운 사용자 추가
            List<SharedUser> finalSharedUsers = new ArrayList<>(uniqueSharedUsers);


            Query query = new Query(Criteria.where("_id").is(id));
            Update update = new Update()
                    .set("sharedDepts", sharedDeptList)
                    .set("sharedUsers", finalSharedUsers) // 병합된 사용자 리스트 설정
                    .set("target", "target");
            mongoTemplate.upsert(query, update, Folder.class);

            propagatePermissions(folder.getPath(),finalSharedUsers,sharedDeptList,1);

            return true;
        }else if(type.equals("file")){

            return true;
        }

        return false;





    }


    public void deletedDepartment(RemoveDepartmentRequestDto request ) {
        if(request.getType().equals("folder")){

            Folder folder = folderMogoRepository.findById(request.getId())
                   .orElseThrow(() -> new IllegalArgumentException("Invalid Folder ID or Type"));
            List<String> departmentIdsToRemove = request.getDeletedDepartments();
            List<ShareDept> sharedDepts = folder.getSharedDepts();
            // 삭제 대상 사용자와 부서 ID 리스트
            List<User> usersToRemove = new ArrayList<>();
            List<SharedUser> sharedUsers = folder.getSharedUsers();
            for(String deptId  : departmentIdsToRemove){
                List<GroupMapper> groupMappers = groupMapperRepository.findGroupMapperByGroup_Id(Long.valueOf(deptId));

                for(GroupMapper gm : groupMappers){
                    usersToRemove.add(gm.getUser());
                }


                sharedDepts.removeIf(sharedDept -> sharedDept.getDeptId().equals(deptId));
                folder.setSharedDepts(sharedDepts);

            }
            // 필요시 usersToRemove에 있는 사용자 삭제 (예: MongoDB에서 업데이트 또는 삭제)
            if (!usersToRemove.isEmpty()) {
                // 사용자 제거 로직 추가 (예: 관련 문서에서 제거)
                log.info("Users to be removed: {}", usersToRemove);
                for (User user : usersToRemove) {
                    sharedUsers.removeIf(sharedUser -> sharedUser.getId().equals(user.getId()));
                }

                folder.setSharedUsers(sharedUsers);

            }

            folderMogoRepository.save(folder);
            propagatePermissions(folder.getPath(),sharedUsers,sharedDepts,1);

            log.info("Departments and related users removed successfully.");

        }

    }


    public void propagatePermissions(String path, List<SharedUser> sharedUserJson,List<ShareDept> shardedDeptJson,int isShared) {
        // 2. 하위 폴더 업데이트
        Query folderQuery = new Query(Criteria.where("path").regex("^" + path + "(/|$)")); // 하위 폴더를 찾기 위한 쿼리
        Update folderUpdate = new Update()
                .set("updatedAt", LocalDateTime.now())
                .set("sharedUsers", sharedUserJson)
                .set("sharedDepts", shardedDeptJson)
                .set("isShared", isShared);
        mongoTemplate.updateMulti(folderQuery, folderUpdate, Folder.class);

        // 3. 하위 폴더 내 모든 파일 업데이트
        Query fileQuery = new Query(Criteria.where("path").regex("^" + path + "(/|$)")); // 하위 폴더 경로 기준으로 파일 검색
        Update fileUpdate = new Update()
                .set("updatedAt", LocalDateTime.now())
                .set("sharedUser", sharedUserJson)
                .set("sharedDept", shardedDeptJson)
                .set("isShared", isShared);
        mongoTemplate.updateMulti(fileQuery, fileUpdate, FileMogo.class);
    }


    public void filePermission(String fileId, String sharedUserJson, String shardedDeptJson, int isShared) {
        FileMogo fileMogo = fileMogoRepository.findById(fileId).orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));
        String parentPath = fileMogo.getPath();
        Query query = new Query(Criteria.where("_id").is(fileId));
        Update fileUpdate = new Update()
                .set("updatedAt", LocalDateTime.now())
                .set("sharedUser", sharedUserJson)
                .set("sharedDept", shardedDeptJson)
                .set("isShared", isShared);
        mongoTemplate.updateMulti(query, fileUpdate, FileMogo.class);
    }
}
