package com.backend.service;


import com.backend.document.drive.FileMogo;
import com.backend.document.drive.Folder;
import com.backend.dto.request.drive.ShareRequestDto;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public void shareddepartment(ShareRequestDto shareRequestDto ) {



    }

    public void sharedDepartment(ShareRequestDto shareRequestDto ,String type, String id) {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Map<String, String>> sharedUserMap = new HashMap<>();
        Map<String, Map<String, String>> sharedDeptMap = new HashMap<>();
        if(shareRequestDto.getDepartments() != null){
            for(ShareRequestDto.ShareEntity group:shareRequestDto.getDepartments()){
                sharedDeptMap.put(""+group.getId(),Map.of("id",String.valueOf(group.getId()),"permission",group.getPermission()));
                String permission =group.getPermission();
               List<GroupMapper> groupMappers =  groupMapperRepository.findGroupMapperByGroup_Id(group.getId());
               for (GroupMapper groupMapper : groupMappers) {

                   long userId = groupMapper.getUser().getId();
                   User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
                   permissionService.savePermission(id,user,type,permission);
                   sharedUserMap.put("" + userId, Map.of(
                           "id", String.valueOf(userId),
                           "authority","아직빈칸",
                           "email",user.getEmail(),
                           "level", user.selectLevelString(),
                               "name", user.getName(),
                           "uid", user.getUid(),
                           "permission", permission));
               }

            }

        }
        if(shareRequestDto.getUsers() != null){
            for(ShareRequestDto.ShareEntity shareEntity : shareRequestDto.getUsers()){
                long userId = shareEntity.getId();
                User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
                String permission = shareEntity.getPermission();
                permissionService.savePermission(id,user, type,  permission);
                sharedUserMap.put("" + userId, Map.of(
                        "id", String.valueOf(userId),
                        "authority","아직빈칸",
                        "email",user.getEmail(),
                        "level", user.selectLevelString(),
                        "name", user.getName(),
                        "uid", user.getUid(),
                        "permission", permission));            }
        }

        try {
            // Map 데이터를 JSON 문자열로 변환
            String sharedUserJson = objectMapper.writeValueAsString(sharedUserMap);
            String shardedDeptJson = objectMapper.writeValueAsString(sharedDeptMap);
            log.info("Shared User JSON: " + sharedUserJson);
            if(type.equals("folder")){
                propagatePermissions(id,sharedUserJson,shardedDeptJson,1);
            }else if(type.equals("file")){
                filePermission(id,sharedUserJson,shardedDeptJson,1);
            }



        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to convert shared user data to JSON", e);
        }


    }


    public void propagatePermissions(String folderId, String sharedUserJson, String shardedDeptJson,int isShared) {
        Folder folder = folderMogoRepository.findById(folderId).orElseThrow(() -> new IllegalArgumentException("Folder not found: " + folderId));
        String parentPath = folder.getPath();
        // 1. 하위 폴더 업데이트
        // 2. 하위 폴더 업데이트
        Query folderQuery = new Query(Criteria.where("path").regex("^" + parentPath + "(/|$)")); // 하위 폴더를 찾기 위한 쿼리
        Update folderUpdate = new Update()
                .set("updatedAt", LocalDateTime.now())
                .set("sharedUser", sharedUserJson)
                .set("sharedDept", shardedDeptJson)
                .set("isShared", isShared);
        mongoTemplate.updateMulti(folderQuery, folderUpdate, Folder.class);

        // 3. 하위 폴더 내 모든 파일 업데이트
        Query fileQuery = new Query(Criteria.where("path").regex("^" + parentPath + "(/|$)")); // 하위 폴더 경로 기준으로 파일 검색
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
