package com.backend.service;


import com.backend.document.drive.FileMogo;
import com.backend.document.drive.Folder;
import com.backend.document.drive.Invitation;
import com.backend.document.drive.ShareLink;
import com.backend.dto.request.drive.*;
import com.backend.entity.folder.DriveSetting;
import com.backend.entity.group.GroupMapper;
import com.backend.entity.user.Alert;
import com.backend.entity.user.User;
import com.backend.repository.GroupMapperRepository;
import com.backend.repository.UserRepository;
import com.backend.repository.drive.*;
import com.backend.repository.user.AlertRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.hpsf.GUID;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    private final ShareLinkRepository shareLinkRepository;
    private final FolderService folderService;
    private final AlertRepository alertRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final DriveSettingRepository driveSettingRepository;


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




                folder.setSharedUsers(savedUser);;

                folderMogoRepository.save(folder);
                messageToSharedUser(savedUser,id);

                List<SharedUser> missingUsers = new ArrayList<>(existingSharedUsers);
                missingUsers.removeAll(savedUser);
                messageToRemove(missingUsers,id);


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

                Map<Long, SharedUser> userMap = new LinkedHashMap<>();

                for (SharedUser user : folder.getSharedUsers()) {
                    userMap.put(user.getId(), user);
                }

                for (SharedUser user : savedUser) {
                    userMap.put(user.getId(), user);
                }

                List<SharedUser> finalSharedUsers = new ArrayList<>(userMap.values());
                folder.setSharedUsers(finalSharedUsers);


                Query query = new Query(Criteria.where("_id").is(id));
                Update update = new Update()
                        .set("sharedUsers", finalSharedUsers) // 병합된 사용자 리스트 설정
                        .set("invitations", saveInvitations);
                mongoTemplate.upsert(query, update, Folder.class);

                propagatePermissions(folder.getPath(),finalSharedUsers,folder.getSharedDepts(),saveInvitations,1);

                messageToSharedUser(savedUser,id);

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
            List<SharedUser> combinedUsers = new ArrayList<>();
            combinedUsers.addAll(folder.getSharedUsers());
            combinedUsers.addAll(sharedUserList);

            List<SharedUser> finalSharedUsers  = combinedUsers.stream()
                    .distinct()
                    .collect(Collectors.toList());

            folder.setSharedUsers(finalSharedUsers );


            Query query = new Query(Criteria.where("_id").is(id));
            Update update = new Update()
                    .set("sharedDepts", sharedDeptList)
                    .set("sharedUsers", finalSharedUsers); // 병합된 사용자 리스트 설정
            mongoTemplate.upsert(query, update, Folder.class);

            propagatePermissions(folder.getPath(),finalSharedUsers,sharedDeptList,folder.getInvitations(),1);

            messageToSharedUser(sharedUserList,id);

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
            messageToSharedUser(sharedUsers,request.getId());

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


    public ShareLink generateToken(String sharedId, String uid) {
        String token = UUID.randomUUID().toString(); // 고유 토큰 생성


        ShareLink shareLink = ShareLink.builder()
                .shared_By(uid)
                .token(token)
                .createAt(LocalDateTime.now())
                .expiry_date(LocalDateTime.now().plusDays(7))
                .sharedId(sharedId)
                .permission("읽기")
                .is_active(true)
                .build();

        ShareLink savedLink = shareLinkRepository.save(shareLink); // 7일 유효

        boolean result = updateSharedLink(savedLink,sharedId);
        if (result){
            return savedLink;
        }else{
            return null;
        }

    }
    public boolean updateSharedLink(ShareLink shareLink,String id){
        Folder rootFolder = folderMogoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found: " + id));


        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update()
                .set("isShared", 1)
                .set("sharedToken", shareLink.getToken())
                .set("updatedAt", new Date());
        mongoTemplate.upsert(query, update, Folder.class);
        rootFolder.updateShareToken(shareLink.getToken());

        // 링크 업데이트 시작
        boolean result = updateFolderAndChildren(rootFolder);

        if(result){
            return true;
        }else{
            return false;
        }
    }

    private boolean updateFolderAndChildren(Folder folder) {
        String sharedtoken = folder.getSharedToken();

        // 현재 폴더의 파일 처리
        List<FileMogo> files =  fileMogoRepository.findAllByFolderId(folder.getId());
        int size = files.size();
        int count =0;
        for (FileMogo file : files) {
            Query query = new Query(Criteria.where("_id").is(file.getId()));
            Update update = new Update()
                    .set("isShared", 1)
                    .set("sharedToken", sharedtoken)
                    .set("updatedAt", new Date());
            mongoTemplate.upsert(query, update, Folder.class);
            count ++;
        }

        // 하위 폴더 재귀적으로 처리
        List<Folder> children = folderMogoRepository.findAllByParentId(folder.getId());
        size += children.size();
        for (Folder child : children) {
            Query query = new Query(Criteria.where("_id").is(child.getId()));
            Update update = new Update()
                    .set("isShared", 1)
                    .set("sharedToken", sharedtoken)
                    .set("updatedAt", new Date());
            mongoTemplate.upsert(query, update, Folder.class);
            updateFolderAndChildren(child);
            count ++;

        }
        if(count == size){
            return true;
        }else{
            return false;
        }
    }

    public boolean validateToken(String token){

        log.info("여기 들어와 "+token);
        Optional<ShareLink> opt= shareLinkRepository.findByToken(token);

        if(opt.isPresent()){


            ShareLink shareLink = opt.get();
            log.info("여기 들어와 shareLink  "+shareLink);
            boolean isExpired = shareLink.isExpired();
            log.info("isExpired  "+isExpired);

            if(isExpired){
                return false;
            }else{
                return true;
            }

        }
        return false;
    }

    public void messageToSharedUser(List<SharedUser> sharedUserList,String id){
        for(SharedUser sharedUser : sharedUserList){
            User user = User.builder()
                    .id(sharedUser.getId())
                    .uid(sharedUser.getUid())
                    .build();
            Optional<DriveSetting> setting = driveSettingRepository.findByUserId(sharedUser.getId());
            if(setting.isPresent() && !setting.get().isShare_notifications()){
                messagingTemplate.convertAndSend(
                        "/topic/folder/updates/"+user.getId(),
                        id
                );
            }else{
                Alert alert = Alert.builder()
                        .user(user)
                        .title("공유")
                        .type(2)
                        .status(2)
                        .createAt(LocalDateTime.now().toString())
                        .content("드라이브가 공유되었습니다.")
                        .build();

                Alert savedAlert = alertRepository.save(alert);
                log.info("공유 메세지 전달!!"+sharedUser.getUid() +"대상 "+id);
                messagingTemplate.convertAndSend(
                        "/topic/folder/updates/"+user.getId(),
                        id
                );
                messagingTemplate.convertAndSendToUser(sharedUser.getId().toString(),"/topic/alerts/", savedAlert.toGetAlarmDto());

            }

        }
    }

    public void messageToRemove(List<SharedUser> sharedUserList,String id){
        for(SharedUser sharedUser : sharedUserList){
            User user = User.builder()
                    .id(sharedUser.getId())
                    .uid(sharedUser.getUid())
                    .build();
            Optional<DriveSetting> setting = driveSettingRepository.findByUserId(sharedUser.getId());
            if(setting.isPresent() && !setting.get().isShare_notifications()){
                messagingTemplate.convertAndSend(
                        "/topic/folder/remove/"+sharedUser.getId(),
                        id
                );
            }else{
                Alert alert = Alert.builder()
                        .user(user)
                        .title("공유 해제")
                        .type(2)
                        .status(2)
                        .createAt(LocalDateTime.now().toString())
                        .content("드라이브가 공유 해제되었습니다.")
                        .build();

                Alert savedAlert = alertRepository.save(alert);
                log.info("공유 메세지 전달!!"+sharedUser.getUid() +"대상 "+id);
                messagingTemplate.convertAndSend(
                        "/topic/folder/remove/"+sharedUser.getId(),
                        id
                );
                messagingTemplate.convertAndSendToUser(sharedUser.getId().toString(),"/topic/alerts/", savedAlert.toGetAlarmDto());

            }

        }
    }
}
