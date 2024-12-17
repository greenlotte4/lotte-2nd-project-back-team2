package com.backend.service;


import com.backend.document.drive.FileMogo;
import com.backend.document.drive.Folder;
import com.backend.document.drive.Invitation;
import com.backend.dto.request.drive.*;
import com.backend.entity.group.GroupMapper;
import com.backend.entity.user.User;
import com.backend.repository.GroupMapperRepository;
import com.backend.repository.UserRepository;
import com.backend.repository.drive.FileMogoRepository;
import com.backend.repository.drive.FolderMogoRepository;
import com.backend.repository.drive.InvitationRepository;
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
    private final InvitationRepository invitationRepository;
    private final EmailService emailService;

    //department
    public boolean shareUser(ShareRequestDto shareRequestDto,String type,String id ) {

        if(shareRequestDto.getUserType().equals("department")){
            if(type.equals("folder")){
                Folder folder = folderMogoRepository.findById(id).orElseThrow(() -> new RuntimeException("Folder not found"));
                List<SharedUser> requset = shareRequestDto.getSharedUsers();
                List<SharedUser> savedUser = new ArrayList<>();

                for(SharedUser sharedUser : requset){
                    User user = userRepository.findById(sharedUser.getId()).orElseThrow(() -> new RuntimeException("User not found"));
                    SharedUser sharedUser1 = SharedUser.builder()
                            .uid(user.getUid())
                            .id(user.getId())
                            .profile(user.getProfileImgPath())
                            .permission(sharedUser.getPermission())
                            .email(user.getEmail())
                            .group(sharedUser.getGroup())
                            .authority(sharedUser.getAuthority())
                            .name(user.getName())
                            .build();

                    savedUser.add(sharedUser1);
                }




                List<SharedUser> existingSharedUsers = folder.getSharedUsers();
                Set<SharedUser> uniqueSharedUsers = new HashSet<>(existingSharedUsers);
                uniqueSharedUsers.addAll(savedUser); // 새로운 사용자 추가
                List<SharedUser> finalSharedUsers = new ArrayList<>(uniqueSharedUsers);

                folder.setSharedUsers(finalSharedUsers);
                folderMogoRepository.save(folder);

                return true;
            }else if(type.equals("file")){
                return true;

            }


        }else if(shareRequestDto.getUserType().equals("individual")){
            List<SharedUser> savedUser = new ArrayList<>();
            List<Invitation> saveInvitations = new ArrayList<>();

            for(SharedUser sharedUser : shareRequestDto.getSharedUsers()){
                Optional<User> user = userRepository.findByEmail(sharedUser.getEmail());


                if(user.isPresent()){
                    User user1 = user.get();


                    String groupName = (user1.getGroupMappers() == null || user1.getGroupMappers().isEmpty())
                            ? "개인"
                            : user1.getGroupMappers().get(0).getGroup().getName();

                    SharedUser users  = SharedUser.builder()
                            .uid(user1.getUid())
                            .id(user1.getId())
                            .profile(user1.getProfileImgPath())
                            .name(user1.getName())
                            .email(user1.getEmail())
                            .authority(user1.getRole().toString())
                            .group(groupName)
                            .permission(sharedUser.getPermission())
                            .build();
                    savedUser.add(users);

                }else{
                    Optional<Invitation> alreadyInvitation =  invitationRepository.findByEmail(sharedUser.getEmail());
                    if(alreadyInvitation.isEmpty()){
                        Invitation invitation = Invitation.builder()
                                .status("PENDING")
                                .type(type)
                                .sharedId(id)
                                .email(sharedUser.getEmail())
                                .permission(sharedUser.getPermission())
                                .createdAt(LocalDateTime.now())
                                .build();

                        invitation.onExpiredAT();
                        Invitation saved = invitationRepository.save(invitation);
                        saveInvitations.add(saved);
                    }


                }

            }
            if(type.equals("folder")){

                Folder folder = folderMogoRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid Folder ID or Type"));

                List<SharedUser> existingSharedUsers = folder.getSharedUsers();
                Set<SharedUser> uniqueSharedUsers = new HashSet<>(existingSharedUsers);
                uniqueSharedUsers.addAll(savedUser); // 새로운 사용자 추가
                List<SharedUser> finalSharedUsers = new ArrayList<>(uniqueSharedUsers);



                Query query = new Query(Criteria.where("_id").is(id));
                Update update = new Update()
                        .set("sharedUsers", finalSharedUsers) // 병합된 사용자 리스트 설정
                        .set("invitations", saveInvitations);
                mongoTemplate.upsert(query, update, Folder.class);

                propagatePermissions(folder.getPath(),finalSharedUsers,folder.getSharedDepts(),saveInvitations,1);

                emailService.sendToInvitation(saveInvitations,folder);


            }


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
                                .authority(user.getRole() != null ? user.getRole().toString() : "default") // 기본 권한 값 설정                            .
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
        log.info("공유유저 리스트!!!"+sharedDeptList);


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

            propagatePermissions(folder.getPath(),finalSharedUsers,sharedDeptList,folder.getInvitations(),1);

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
            propagatePermissions(folder.getPath(),sharedUsers,sharedDepts,folder.getInvitations(),1);

            log.info("Departments and related users removed successfully.");

        }

    }


    public void propagatePermissions(String path, List<SharedUser> sharedUserJson,List<ShareDept> shardedDeptJson,List<Invitation> Invitations,int isShared) {
        // 2. 하위 폴더 업데이트
        Query folderQuery = new Query(Criteria.where("path").regex("^" + path + "(/|$)")); // 하위 폴더를 찾기 위한 쿼리
        Update folderUpdate = new Update()
                .set("updatedAt", LocalDateTime.now())
                .set("sharedUsers", sharedUserJson)
                .set("sharedDepts", shardedDeptJson)
                .set("Invitations",Invitations)
                .set("isShared", isShared);
        mongoTemplate.updateMulti(folderQuery, folderUpdate, Folder.class);

        // 3. 하위 폴더 내 모든 파일 업데이트
        Query fileQuery = new Query(Criteria.where("path").regex("^" + path + "(/|$)")); // 하위 폴더 경로 기준으로 파일 검색
        Update fileUpdate = new Update()
                .set("updatedAt", LocalDateTime.now())
                .set("sharedUser", sharedUserJson)
                .set("sharedDept", shardedDeptJson)
                .set("Invitations",Invitations)
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

    public  Map<String, Object> invitationInvaild(String invitationId,String shareUid){
        Map<String, Object> response = new HashMap<>();
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElse(null);
        User loginUser = userRepository.findByUid(shareUid)
                .orElse(null);

        if (invitation == null || loginUser == null) {
            response.put("status", "fail");
            response.put("message", "초대장 또는 사용자를\n 찾을 수 없습니다.");
            return response;
        }

        if (!loginUser.getEmail().equals(invitation.getEmail())) {
            response.put("status", "fail");
            response.put("message", "로그인한 사용자와 초대장의 이메일이 \n일치하지 않습니다.");
            return response;
        }

        if (invitation.getExpiredAt().isBefore(LocalDateTime.now())) {
            invitation.setState("expired"); // 상태를 'expired'로 설정
            response.put("status", "fail");
            response.put("message", "이 초대장은 \n만료되었습니다.");
            invitationRepository.save(invitation); // 상태 업데이트
            return response;
        }

        // 이미 공유된 사용자 여부 검증 (예시)
        boolean alreadyShared = checkIfAlreadyShared(invitation.getSharedId(), loginUser.getUid());
        if (alreadyShared || invitation.getStatus().equals("ACCEPTED")) {
            response.put("status", "fail");
            response.put("message", "이 초대장은 \n이미 공유되었습니다.");
            return response;
        }


        String groupName = (loginUser.getGroupMappers() == null || loginUser.getGroupMappers().isEmpty())
                ? "개인"
                : loginUser.getGroupMappers().get(0).getGroup().getName();
        SharedUser sharedUser = SharedUser.builder()
                .email(invitation.getEmail())
                .id(loginUser.getId())
                .group(groupName)
                .name(loginUser.getName())
                .permission(invitation.getPermission())
                .profile(loginUser.getProfileImgPath())
                .uid(loginUser.getUid())
                .authority(loginUser.getRole() != null ? loginUser.getRole().toString() : "default") // 기본 권한 값 설정                            .
                .build();

        ShareRequestDto shareRequestDto = ShareRequestDto.builder()
                .userType("individual")
                .sharedUsers(Collections.singletonList(sharedUser))
                .build();


        boolean result = shareUser(shareRequestDto,invitation.getType(),invitation.getSharedId());

        if (result) {
            response.put("sharedId", invitation.getSharedId());
            response.put("status", "success");
            response.put("message", "Invitation validated successfully.");
            invitation.setState("accepted");
            invitationRepository.save(invitation); // 상태 업데이트

            return response;
        } else {
            response.put("status", "fail");
            response.put("message", "Failed to process invitation.");
            return response;
        }


    }

    // 이미 공유된 사용자인지 검증하는 메서드
    private boolean checkIfAlreadyShared(String sharedId, String uid) {
        // 공유된 사용자 목록 조회 및 검증
        return  folderMogoRepository.findByIdAndSharedUsersUid(sharedId, uid).isPresent();
    }
}
