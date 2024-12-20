package com.backend.controller;


import com.backend.document.drive.FileMogo;
import com.backend.dto.request.FileRequestDto;
import com.backend.dto.request.drive.*;
import com.backend.dto.response.UserDto;
import com.backend.dto.response.drive.FolderDto;
import com.backend.document.drive.Folder;
import com.backend.dto.response.drive.FolderResponseDto;
import com.backend.entity.user.User;
import com.backend.service.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@Log4j2
@CrossOrigin(origins = "http://localhost:8010")
@RequestMapping("/api/drive")
@RequiredArgsConstructor
public class DriveController {


    private final FolderService folderService;
    private final UserService userService;
    private final PermissionService permissionService;
    private final SftpService sftpService;
    private final ThumbnailService thumbnailService;
    private final ProgressService progressService;


    //드라이브생성 => 제일 큰 폴더
    @PostMapping("/newDrive")
    public void createDrive(@RequestBody NewDriveRequest newDriveRequest,HttpServletRequest request) {
        log.info("New drive request: " + newDriveRequest);
        String uid= (String) request.getAttribute("uid");
        newDriveRequest.setOwner(uid);
        Folder forFolder = folderService.getFolderName("ROOT",uid);

        //부모폴더생성
        if(forFolder == null) {
            NewDriveRequest rootdrive = NewDriveRequest.builder()
                    .owner(newDriveRequest.getOwner())
                    .name(uid)
                    .type("ROOT")
                    .driveMaster(uid)
                    .description(uid+"의 드라이브")
                    .build();
            String rootId =folderService.createRootDrive(rootdrive);
            newDriveRequest.setParentId(rootId);
            permissionService.addPermission(rootId,uid,"folder",newDriveRequest.getPermissions());
        }else{
            newDriveRequest.setParentId(forFolder.getId());
        }

        //폴더생성

        newDriveRequest.setType("DRIVE");
        String forderId = folderService.createFolder(newDriveRequest);

        //권한설정 저장


    }


    //드라이브 안의 폴더 생성
    @PostMapping("/newFolder")
    public ResponseEntity createFolder(@RequestBody NewDriveRequest newDriveRequest,HttpServletRequest request) {
        log.info("New drive request: " + newDriveRequest);

        String uid= (String) request.getAttribute("uid");
        newDriveRequest.setOwner(uid);

        FolderDto folderDto = folderService.getParentFolder(newDriveRequest.getParentId());

        //읽기 권한시 업로드 불가능
        if(!folderDto.getOwnerId().equals(uid)){
            Optional<String>  opt = folderDto.getSharedUsers()
                    .stream()
                    .filter(sharedUser -> sharedUser.getUid().equals(uid)) // uid가 일치하는 사용자 찾기
                    .map(sharedUser -> sharedUser.getPermission()) // 권한 추출
                    .findFirst(); // 첫 번째 일치하는 권한 반환
            if(opt.isPresent() && opt.get().equals("읽기")){
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("폴더 생성 권한이 없습니다.");
            }

        }


        newDriveRequest.setParentFolder(folderDto);
        newDriveRequest.setType("FOLDER");
        newDriveRequest.setShareDepts(folderDto.getShareDepts());
        newDriveRequest.setSharedUsers(folderDto.getSharedUsers());

        String folderId = folderService.createFolder(newDriveRequest);

        //권한설정 저장
        permissionService.addPermission(folderId,uid,"folder",newDriveRequest.getPermissions());
        if(folderId != null) {
            return ResponseEntity.ok().body(folderId);

        }else{
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("폴더 생성에 실패했습니다.");
        }
    }

    //사이드바  폴더 리스트 불러오기
    @GetMapping("/folders")
    public ResponseEntity getDriveList(HttpServletRequest request,@RequestParam(required = false) String uid) {


        if(uid == null) {
            uid= (String) request.getAttribute("uid");
        }

        if (uid == null || uid.isEmpty()) {
            return ResponseEntity.badRequest().body("UID is required.");
        }
        log.info("Get drive list  uid:"+uid);
        Folder rootFolder = folderService.getFolderName("ROOT",uid);
        if (rootFolder == null) {
            return ResponseEntity.ok().body("No folders found.");
        }
        List<FolderDto> folderDtoList =  folderService.getFoldersByUid(uid, rootFolder.getId());


        List<FolderDto> shareFolderList= folderService.sharedFolder(uid);
        log.info("공유된 내용이 없어?",shareFolderList);

        FolderResponseDto folderResponseDto  = FolderResponseDto.builder()
                .folderDtoList(folderDtoList)
                .shareFolderDtoList(shareFolderList)
                .uid(uid)
                .build();

        return ResponseEntity.ok().body(folderResponseDto);

    }

    @GetMapping("/size")
    public ResponseEntity getSize(HttpServletRequest request){
        String uid= (String) request.getAttribute("uid");
        if (uid == null || uid.isEmpty()) {
            return ResponseEntity.badRequest().body("UID is required.");
        }
        long  result  = sftpService.calculatedSize(uid);

        return  ResponseEntity.ok().body(result);
    }


    //각 폴더의 컨텐츠 가져오기
    @GetMapping("/folder-contents")
    public ResponseEntity<Map<String, Object>> getFolderContents(HttpServletRequest request,@RequestParam String folderId,@RequestParam(required = false) String ownerId){
        Map<String,Object> response = new HashMap<>();

        FolderDto parentFolder = folderService.getParentFolder(folderId);
        List<UserDto> sharedUsersWithDetails = new ArrayList<>();

        //폴더 가져오기
        String uid = (String) request.getAttribute("uid");
        Long id = (Long) request.getAttribute("id");
        List<FolderDto> subFolders = new ArrayList<>();
        List<FileRequestDto> files = new ArrayList<>();
        if(parentFolder.getOwnerId().equals(uid)){
            subFolders = folderService.getSubFolders(uid,folderId);
            files = folderService.getFiles(folderId);

        }else{
            List<SharedUser> users = parentFolder.getSharedUsers();
            boolean isSharedUser = users.stream().anyMatch(user -> user.getId().equals(id));

            if(isSharedUser){
                subFolders = folderService.getSharedSubFolders(id,folderId);
                files = folderService.getFiles(folderId);

            }

        }


        response.put("files",files);
        response.put("parentFolder",parentFolder);
        response.put("subFolders", subFolders);
        response.put("uid",uid);
        log.info("subFolders:"+subFolders);
        log.info("files:"+files);


        return ResponseEntity.ok().body(response);

    }

    //폴더 이름 바꾸기
    @PutMapping("/folder/{text}/rename")
    public ResponseEntity renameFolder(@RequestParam String newFolderName,@PathVariable String text){
        log.info("Rename folder name:"+newFolderName);

        folderService.updateFolder(text, newFolderName);

        return null;
    }


    @PutMapping("/rename")
    public  ResponseEntity<?> changeName(@RequestBody RenameRequest renameRequest) {
        String id = renameRequest.getId();
        String type = renameRequest.getType();
        String newName = renameRequest.getNewName();
        log.info("ID: " + id + ", Type: " + type + ", New Name: " + newName);

        if(type.equals("folder")){
            folderService.reNameFolder(renameRequest);
        }else if(type.equals("file")){
            folderService.reNameFile(id, newName);
        }

        return null;


    }

    //폴더 이름 바꾸기
    @PutMapping("/folder/{folderId}/move")
    public ResponseEntity moveFolder(@RequestBody MoveFolderRequest moveFolderRequest, @PathVariable String folderId){
        log.info("move folder name:"+moveFolderRequest);

        double changedOrder =  folderService.updateFolder(moveFolderRequest);

        if(moveFolderRequest.getOrder()== changedOrder){
            return ResponseEntity.ok().body("Folder updated successfully");
        }else{
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Folder update failed");
        }


    }



    // 파일 업로드 처리
    @Transactional
    @PostMapping("/upload/{folderId}")
    public ResponseEntity<?> uploadFiles( @PathVariable String folderId,
                                          @RequestParam("files") List<MultipartFile> files,
                                          @RequestParam("relativePaths") List<String> relativePaths, // 폴더 경로 배열 수신
                                          @RequestParam("fileMaxOrder") double fileMaxOrder,
                                          @RequestParam("folderMaxOrder") double folderMaxOrder,
                                          @RequestParam("uid") String uid,HttpServletRequest request
    )  {
        double fileOrder = fileMaxOrder;
        double folderOrder = folderMaxOrder;
        FolderDto parentFolder = folderService.getParentFolder(folderId);


        //읽기 권한시 업로드 불가능
        if(!parentFolder.getOwnerId().equals(uid)){
           Optional<String>  opt = parentFolder.getSharedUsers()
                   .stream()
                   .filter(sharedUser -> sharedUser.getUid().equals(uid)) // uid가 일치하는 사용자 찾기
                   .map(sharedUser -> sharedUser.getPermission()) // 권한 추출
                   .findFirst(); // 첫 번째 일치하는 권한 반환
            if(opt.isPresent() && opt.get().equals("읽기")){
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("업로드 권한이 없습니다.");
            }

        }

        log.info("relativePaths:"+relativePaths);

        try {
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                String relativePath = (relativePaths != null && relativePaths.size() > i)
                        ? relativePaths.get(i)
                        : null;

                // 상대 경로가 없는 경우 최상위 폴더에 저장
                if (relativePath == null || relativePath.isEmpty() ) {
                    folderService.saveFileToFolder(file, folderId, fileOrder, uid);
                    fileOrder += 100;
                    continue;
                }

                // 상대 경로를 '/'로 분리하고 공백 제거
                String[] folderList = Arrays.stream(relativePath.split("/"))
                        .filter(folderName -> folderName != null && !folderName.trim().isEmpty())
                        .toArray(String[]::new);
                if(folderList[0].equals(".")){
                    folderService.saveFileToFolder(file, folderId, fileOrder, uid);
                    fileOrder += 100;
                    continue;
                }

                String currentParentId = folderId; // 초기 Parent ID는 최상위 폴더 ID

                for (String folder : folderList) {
                    String folderOrFileName = folder;

                        Folder existingFolder = folderService.existFolder(folderOrFileName, currentParentId);

                        if (existingFolder != null) {
                            currentParentId = existingFolder.getId(); // 기존 폴더 사용
                            continue;
                        }

                        // 새로운 폴더 생성
                        NewDriveRequest newDriveRequest = NewDriveRequest.builder()
                                .name(folderOrFileName)
                                .owner(uid)
                                .parentId(currentParentId)
                                .parentFolder(parentFolder)
                                .order(folderOrder)
                                .build();
                        String newFolderId = folderService.createFolder(newDriveRequest);
                        currentParentId = newFolderId; // 새로 생성된 폴더 ID를 현재 Parent ID로 설정
                        folderOrder += 100;

                }
                folderService.saveFileToFolder(file, currentParentId, fileOrder, uid);
                fileOrder += 100;
                updateAndSendProgress(uid, i + 1, files.size());

            }
            return ResponseEntity.ok("파일 업로드 성공");
        } catch (MaxUploadSizeExceededException e) {
            log.error("파일 크기 초과: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("파일 크기가 허용된 최대 크기를 초과했습니다.");
        } catch (Exception e) {
            log.error("파일 업로드 중 오류 발생: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("파일 업로드 중 오류가 발생했습니다. 다시 시도해주세요.");
        }

    }

    //zip파일 생성하기
    @GetMapping("/generateZip/{folderId}")
    public ResponseEntity downloadFile(@PathVariable String folderId){
        log.info("Download file:"+folderId);
        Map<String,Object> response = new HashMap<>();
        String result = folderService.makeZipfolder(folderId);

        if(result != null){
            response.put("zipName",result);
            return ResponseEntity.ok().body(response);
        }else{
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body("Folder Zip failed");
        }
    }


    //폴더 삭제(status 변경만)
    @DeleteMapping("/{type}/delete/{Id}")
    public ResponseEntity deleteFolder(@PathVariable String Id,@PathVariable String type ,@RequestParam String path
            ,@RequestParam(required = false) String parentId
            ,HttpServletRequest request){
        Map<String ,String > result = new HashMap<>();
        log.info("Delete folder:"+Id+" path : "+path);
        String currentUser = (String) request.getAttribute("uid");
        FolderDto parentFolder = null;
        if(parentId == null || parentId.isEmpty()){
            parentFolder =folderService.getRootFolder(currentUser);
        }else{
            parentFolder = folderService.getParentFolder(parentId);
        }

        //owner == uid일치시
        if(parentFolder.getOwnerId().equals(currentUser)){
            result =  folderService.goToTrash(Id,type,currentUser,"모든");
        }else{
            Optional<String>  opt = parentFolder.getSharedUsers()
                    .stream()
                    .filter(sharedUser -> sharedUser.getUid().equals(currentUser)) // uid가 일치하는 사용자 찾기
                    .map(sharedUser -> sharedUser.getPermission()) // 권한 추출
                    .findFirst(); // 첫 번째 일치하는 권한 반환

            if(opt.isPresent() && opt.get().equals("읽기")){
                return ResponseEntity.badRequest().body("삭제 권한이 없습니다.");
            }else if(opt.isPresent() && opt.get().equals("수정")){
               result = folderService.goToTrash(Id,type,currentUser,"수정");
            }else if(opt.isPresent() && opt.get().equals("모든")){
                result = folderService.goToTrash(Id,type,currentUser,"모든");

            }

        }
        String response= result.get("result");
        String message =  result.get("message");
        if(response.equals("success")){
            return ResponseEntity.ok().body(message);
        }else{
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
        }





    }


    @DeleteMapping("/selected/delete")
    public ResponseEntity deleteSelectedFolder(@RequestBody DeletedRequest deletedRequest){
        log.info("Delete selected folder:"+deletedRequest);
        try {
            folderService.seletedDeleted(deletedRequest);
            return ResponseEntity.ok("Selected folders and files deleted successfully");
        } catch (IllegalArgumentException ex) {
            log.error("Invalid request: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            log.error("Error while deleting folders or files", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete selected items");
        }
    }


    @PutMapping("/{type}/{id}/favorite")
    public ResponseEntity setFavorite(@PathVariable String type,@PathVariable String id ){
        Map<String, Integer> respone= new HashMap<>();
        int result=0;
            result= folderService.favorite(id,type);



        respone.put("result",result);

        return ResponseEntity.ok().body(respone);

    }

    @GetMapping("/favorite")
    public ResponseEntity isFavorite(HttpServletRequest request){
        log.info("즐겨찾기 목록!!!");
        String uid = (String)request.getAttribute("uid");
        Map<String,Object> response = new HashMap<>();

        List<FolderDto> subFolders = folderService.isFavorite(uid);
        List<FileRequestDto> files = folderService.isFavoriteFile(uid);

        response.put("files",files);
        response.put("subFolders", subFolders);
        response.put("uid",uid);
        log.info("isFavorite subFolders:"+subFolders);
        log.info(" isFavorite files:"+files);


        return ResponseEntity.ok().body(response);
    }


    @GetMapping("/latest")
    public ResponseEntity latest(HttpServletRequest request){
        log.info("최근문서 목록!!!");
        String uid = (String)request.getAttribute("uid");
        Map<String,Object> response = new HashMap<>();

        List<FolderDto> subFolders = folderService.latestFolder(uid);
        List<FileRequestDto> files = folderService.latestFile(uid);

        response.put("files",files);
        response.put("subFolders", subFolders);
        response.put("uid",uid);
        log.info("latest subFolders:"+subFolders);
        log.info(" latest files:"+files);


        return ResponseEntity.ok().body(response);
    }


    @GetMapping("/trash")
    public ResponseEntity trash(HttpServletRequest request){
        log.info("휴지통 목록!!!");
        String uid = (String)request.getAttribute("uid");
        Map<String,Object> response = new HashMap<>();

        List<FolderDto> subFolders = folderService.trashFolder(uid);
        List<FileRequestDto> files = folderService.trashFile(uid);

        response.put("files",files);
        response.put("subFolders", subFolders);
        response.put("uid",uid);
        log.info("trash subFolders:"+subFolders);
        log.info(" trash files:"+files);


        return ResponseEntity.ok().body(response);
    }

    @DeleteMapping("/cleanAll")
    public ResponseEntity cleanAll(HttpServletRequest request){
        log.info("휴지통비우기 시작!!!!!!");
        String uid = (String)request.getAttribute("uid");

        List<FolderDto> subFolders = folderService.trashFolder(uid);
        List<FileRequestDto> files = folderService.trashFile(uid);

        int size = subFolders.size() + files.size();
        if(size==0){
            log.info("여기 비어있음");
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body("empty");
        }

        DeletedRequest deletedRequest = DeletedRequest.builder()
                .fileDtos(files)
                .subFolders(subFolders)
                .build();
        boolean result = folderService.deleteAll(deletedRequest);

        if(result){
            return ResponseEntity.ok().body("Delete all files successfully");
        }else{
            return ResponseEntity.badRequest().body("Delete all files failed");
        }

    }

    //폴더 복구,
    @DeleteMapping("/{type}/restore/{id}")
    public ResponseEntity restore(@PathVariable String type,@PathVariable String id){
        log.info("복구 로직 시작"+type+"Id "+id);
        boolean result = folderService.restore( type,id);

        return ResponseEntity.ok().body(result);
    }

    @DeleteMapping("/{type}/permanent/{id}")
    public ResponseEntity Permanent(@PathVariable String type,@PathVariable String id){
        log.info("삭제 로직 시작"+type+"Id "+id);
        boolean result = folderService.delete(type,id);
        return ResponseEntity.ok().body(result);
    }



    //webSocket

    private final SimpMessagingTemplate messagingTemplate;

    // 진행률 전송 메서드
    public void sendUploadProgress(String userId, int progress) {
        String destination = "/topic/progress/uploads/" + userId;
        messagingTemplate.convertAndSend(destination, progress);
    }

    private void updateAndSendProgress(String userId, int current, int total) {
        int progress = (int) ((current / (double) total) * 100);
        sendUploadProgress(userId, progress);
    }

    @MessageMapping("/topic/progress/uploads/{uid}")
    public void testProgress(String message, @PathVariable String uid) {
        log.info("WebSocket 전송 경로: /topic/progress/uploads/{}", uid);
        int progress = 50; // 테스트용 진행률
        log.info("Received WebSocket message: {}", message);
        messagingTemplate.convertAndSend("/topic/progress/uploads/" + uid, progress);
    }




}
