package com.backend.controller;


import com.backend.document.drive.FileMogo;
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


    //드라이브생성 => 제일 큰 폴더
    @PostMapping("/newDrive")
    public void createDrive(@RequestBody NewDriveRequest newDriveRequest,HttpServletRequest request) {
        log.info("New drive request: " + newDriveRequest);
        String uid= (String) request.getAttribute("uid");
        newDriveRequest.setOwner(uid);
        Folder forFolder = folderService.getFolderName(uid);

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
    public void createFolder(@RequestBody NewDriveRequest newDriveRequest,HttpServletRequest request) {
        log.info("New drive request: " + newDriveRequest);

        String uid= (String) request.getAttribute("uid");
        newDriveRequest.setOwner(uid);

        FolderDto folderDto = folderService.getParentFolder(newDriveRequest.getParentId());
        newDriveRequest.setParentFolder(folderDto);
        newDriveRequest.setType("FOLDER");
        String folderId = folderService.createFolder(newDriveRequest);

        //권한설정 저장
        permissionService.addPermission(folderId,uid,"folder",newDriveRequest.getPermissions());


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
        long  result  = sftpService.calculatedSize(uid);


        FolderResponseDto folderResponseDto  = FolderResponseDto.builder()
                .folderDtoList(folderDtoList)
                .uid(uid)
                .size(result)
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


    @PutMapping("/{type}/{id}/favorite")
    public ResponseEntity setFavorite(@PathVariable String type,@PathVariable String id ){
        Map<String, Integer> respone= new HashMap<>();
        int result=0;
        if(type.equals("folder")){
            result= folderService.favorite(id);

        }else if(type.equals("file")){

        }
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


}
