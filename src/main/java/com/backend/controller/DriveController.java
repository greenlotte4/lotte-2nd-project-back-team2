package com.backend.controller;


import com.backend.dto.request.FileRequestDto;
import com.backend.dto.request.drive.MoveFolderRequest;
import com.backend.dto.request.drive.NewDriveRequest;
import com.backend.dto.request.drive.RenameRequest;
import com.backend.dto.response.drive.FolderDto;
import com.backend.document.drive.Folder;
import com.backend.dto.response.drive.FolderResponseDto;
import com.backend.entity.user.User;
import com.backend.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @PostMapping("/newDrive")
    public void createDrive(@RequestBody NewDriveRequest newDriveRequest,HttpServletRequest request) {
        log.info("New drive request: " + newDriveRequest);
        String uid= (String) request.getAttribute("uid");
        newDriveRequest.setOwner(uid);
        User currentUser = userService.getUserByuid(newDriveRequest.getOwner());
        Folder forFolder = folderService.getFolderName(currentUser.getUid());
        if(forFolder == null) {
            NewDriveRequest rootdrive = NewDriveRequest.builder()
                    .owner(newDriveRequest.getOwner())
                    .name(currentUser.getUid())
                    .driveMaster(currentUser.getUid())
                    .description(currentUser.getUid()+"의 드라이브")
                    .build();
            String rootId =folderService.createRootDrive(rootdrive);
            newDriveRequest.setParentId(rootId);
            permissionService.createPermission(rootId,currentUser);
        }else{
            newDriveRequest.setParentId(forFolder.getId());
        }

        //폴더생성
        String forderId = folderService.createDrive(newDriveRequest);

        //권한설정 저장
        permissionService.createPermission(forderId,currentUser);


    }


    @PostMapping("/newFolder")
    public void createFolder(@RequestBody NewDriveRequest newDriveRequest,HttpServletRequest request) {
        log.info("New drive request: " + newDriveRequest);

        String uid= (String) request.getAttribute("uid");
        newDriveRequest.setOwner(uid);

        FolderDto folderDto = folderService.getParentFolder(newDriveRequest.getParentId());
        newDriveRequest.setParentFolder(folderDto);
        User currentUser = userService.getUserByuid(newDriveRequest.getOwner());

        String folderId = folderService.createDrive(newDriveRequest);

        //권한설정 저장
        permissionService.createPermission(folderId,currentUser);


    }

    //사이드바  폴더 리스트 불러오기
    @GetMapping("/folders")
    public ResponseEntity getDriveList(HttpServletRequest request,@RequestParam(required = false) String uid) {


        if(uid == null) {
            uid= (String) request.getAttribute("uid");
        }
        uid="worker1";
        log.info("uid1!!"+uid);

        if (uid == null || uid.isEmpty()) {
            return ResponseEntity.badRequest().body("UID is required.");
        }
        log.info("Get drive list  uid:"+uid);
        Folder rootFolder = folderService.getFolderName(uid);
        if (rootFolder == null) {
            return ResponseEntity.ok().body("No folders found.");
        }
        List<FolderDto> folderDtoList =  folderService.getFoldersByUid(uid, rootFolder.getId());
        log.info("folderLIst!!!!"+folderDtoList);
        long size = sftpService.calculatedSize(uid);


        FolderResponseDto folderResponseDto  = FolderResponseDto.builder()
                .folderDtoList(folderDtoList)
                .uid(uid)
                .size(size)
                .build();

        return ResponseEntity.ok().body(folderResponseDto);

    }


    //각 폴더의 컨텐츠 가져오기
    @GetMapping("/folder-contents")
    public ResponseEntity<Map<String, Object>> getFolderContents(HttpServletRequest request,@RequestParam String folderId,@RequestParam(required = false) String ownerId){
        Map<String,Object> response = new HashMap<>();
        //폴더 가져오기
        String uid = (String) request.getAttribute("uid");
        List<FolderDto> subFolders = folderService.getSubFolders(uid,folderId);

        //파일 가져오기
        List<FileRequestDto> files = folderService.getFiles(folderId);


        response.put("files",files);
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
        System.out.println("ID: " + id + ", Type: " + type + ", New Name: " + newName);

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
    @PostMapping("/upload/{folderId}")
    public ResponseEntity<?> uploadFiles( @PathVariable String folderId,
                                          @RequestParam("file") List<MultipartFile> files,
                                          @RequestParam("maxOrder") double maxOrder,
                                          @RequestParam("uid") String uid
    )  {

        log.info("Upload files"+files);
        log.info("folderId:"+folderId);

        folderService.uploadFiles(files,folderId,maxOrder,uid);

        return null;
    }


    //폴더 삭제
    @DeleteMapping("/folder/delete/{folderId}")
    public ResponseEntity deleteFolder(@PathVariable String folderId,@RequestParam String path){

        log.info("Delete folder:"+folderId+" path : "+path);
        boolean result = folderService.goToTrash(folderId,"folder");

        if(result){
            return ResponseEntity.ok().body("Folder deleted successfully");
        }else{
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body("Folder delete failed");
        }
    }


}
