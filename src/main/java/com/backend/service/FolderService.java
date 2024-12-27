package com.backend.service;


import com.backend.document.drive.Restore;
import com.backend.document.drive.ShareLink;
import com.backend.dto.request.FileRequestDto;
import com.backend.dto.request.drive.*;
import com.backend.dto.response.drive.FolderDto;
import com.backend.document.drive.FileMogo;
import com.backend.document.drive.Folder;
import com.backend.dto.response.drive.NewNameResponseDto;
import com.backend.entity.folder.DriveSetting;
import com.backend.repository.drive.DriveSettingRepository;
import com.backend.repository.drive.FileMogoRepository;
import com.backend.repository.drive.FolderMogoRepository;
import com.backend.repository.drive.RestoreRepository;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.internal.bulk.UpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import javax.swing.text.html.Option;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class FolderService {

    private static final String BASE_UPLOAD_DIR = "/data/uploads/";
    private final SftpService sftpService;
    private final UserService userService;
    private final FolderMogoRepository folderMogoRepository;
    private final FileMogoRepository fileMogoRepository;
    private final ThumbnailService thumbnailService;
    private final MongoTemplate mongoTemplate;
    private final RestoreRepository restoreRepository;
    private final DriveSettingRepository driveSettingRepository;
    private String fileServerUrl = "http://43.202.45.49:90/local/upload/create-folder";


    @Value("${spring.servlet.multipart.location}")
    private String uploadPath;


    public String createFolder(NewDriveRequest request){
        String uid = request.getOwner();
        NewNameResponseDto makeDrive = null;

        if(request.getParentFolder() != null){
            FolderDto folderDto = request.getParentFolder();
            Folder exisitingFolder = folderMogoRepository.findFolderByNameAndParentIdAndStatusIsNot(request.getName(),folderDto.getId(),0);
            if(exisitingFolder != null){
                log.info("이미 존재하는 폴더 :{}",exisitingFolder.getName());
                return exisitingFolder.getId();
            }
            makeDrive = sftpService.createNewFolder(request.getName(), folderDto.getPath());


        }else{
            makeDrive = sftpService.createFolder(request.getName(),uid);

        }

        log.info("결과!!!!"+makeDrive);

        if(makeDrive != null){
           Folder folder =  Folder.builder()
                   .name(request.getName())
                   .order((request.getOrder()+1.0)*100) // 널 체크
                   .parentId(request.getParentId())
                   .path(makeDrive.getPath())
                   .folderUUID(makeDrive.getFolderUUID())
                   .type(request.getType())
                   .ownerId(uid)
                   .description(request.getDescription())
                   .status(1)
                   .sharedUser("[]")
                   .isShared(request.getIsShared())
                   .linkSharing(request.getLinkSharing())
                   .createdAt(LocalDateTime.now())
                   .updatedAt(LocalDateTime.now())
                   .sharedDepts(request.getShareDepts())
                   .sharedUsers(request.getSharedUsers())
                   .build();


           Folder savedFolder =  folderMogoRepository.save(folder);

           return savedFolder.getId();

        }
        return null;

    }

    //사용자 Root 폴더 생성 메서드
    public String createRootDrive(NewDriveRequest request){

        String uid = request.getOwner();
        String makeDrivePath =  sftpService.createRootFolder(request.getName(),uid);

        log.info("결과!!!!"+makeDrivePath);

        if(makeDrivePath != null){
            Folder folder =  Folder.builder()
                    .name(request.getName())
                    .folderUUID(null)
                    .order(0.0)
                    .parentId(null)
                    .type("ROOT")
                    .path(makeDrivePath)
                    .ownerId(uid)
                    .description(request.getDescription())
                    .status(1)
                    .isShared(request.getIsShared())
                    .linkSharing(request.getLinkSharing())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            Folder savedFolder =  folderMogoRepository.save(folder);

            return savedFolder.getId();

        }
        return null;

    }

    public DriveSetting insertDriveSetting(String uId, Long userId){

       Optional<DriveSetting> existing =  driveSettingRepository.findByUserId(userId);
           if(existing.isPresent()){
               return existing.get();
           }

            DriveSetting driveSetting = DriveSetting.builder()
                    .userUid(uId)
                    .userId(userId)
                    .storage_alerts(true)
                    .share_notifications(true)
                    .updateAt(LocalDateTime.now())
                    .drive_updates(true)
                    .build();

            return driveSettingRepository.save(driveSetting);

    }


    public List<FolderDto> getFoldersByUid(String uid,String parentId){
        List<Folder> folders = folderMogoRepository.findByOwnerIdAndParentIdAndStatusIsNot(uid,parentId,0);
        log.info("폴더 리스트!!!!"+folders);
            List<FolderDto> folderDtos = folders.stream().map(folder -> {
                FolderDto folderDto = FolderDto.builder()
                        .id(folder.getId())
                        .name(folder.getName())
                        .order(folder.getOrder())
                        .ownerId(folder.getOwnerId())
                        .description(folder.getDescription())
                        .path(folder.getPath())
                        .createdAt(folder.getCreatedAt())
                        .isShared(folder.getIsShared())
                        .isPinned(folder.getIsPinned())
                        .sharedDept(folder.getSharedDept())
                        .sharedUser(folder.getSharedUser())
                        .status(folder.getStatus())
                        .linkSharing(folder.getLinkSharing())
                        .updatedAt(folder.getUpdatedAt())
                        .sharedUsers(folder.getSharedUsers())
                        .shareDepts(folder.getSharedDepts())
                        .parentId(parentId)
                        .build();
                log.info(folderDto.toString());
                return folderDto;
            }).collect(Collectors.toList());
            return folderDtos;


    }


    public List<FolderDto> sharedFolder(String uid){
        List<Folder> folders = folderMogoRepository.findBySharedUsersUidAndStatusIsNot(uid,0);

        return folders.stream().map(Folder::toDTO).collect(Collectors.toList());
    }

    public DriveSettingDto getDriveSetting(String uid,Long id){
        DriveSetting savedDriveSetting = insertDriveSetting(uid,id);
        return savedDriveSetting.toDto();
    }

    public DriveSettingDto updateSetting(String uid, Long id, DriveSettingDto driveSettingDto){
        DriveSetting driveSetting = insertDriveSetting(uid, id);

        String setting = driveSettingDto.getSetting();
        boolean value = driveSettingDto.getValue();

        if(setting.equals("fileUpdates")){
            driveSetting.setDrive_updates(value);
        }else if(setting.equals("shareNotifications")){
            driveSetting.setShare_notifications(value);
        }else if(setting.equals("storageAlerts")){
            driveSetting.setStorage_alerts(value);
        }else{
            return null;
        }

       DriveSetting updateSetting = driveSettingRepository.save(driveSetting);
        return updateSetting.toDto();


    }


    public Folder getFolderName(String type,String uid){
            Optional<Folder> opt =  folderMogoRepository.findByTypeAndOwnerId(type,uid);
        if(opt.isPresent()){
            return opt.get();
        }else{
            return null;
        }
    }
    public Folder existFolder(String name,String parentId){
        return folderMogoRepository.findFolderByNameAndParentIdAndStatusIsNot(name,parentId,0);
    }

    public List<FolderDto> getSubFolders(String ownerId, String folderId){
        List<Folder> folders =folderMogoRepository.findByParentIdAndStatusIsNotOrderByOrderDesc(folderId,0);
        return folders.stream().map(Folder::toDTO).collect(Collectors.toList());
    }

    public List<FolderDto> getSharedSubFolders(Long id, String folderId){
        List<Folder> folders =folderMogoRepository.findByParentIdAndStatusIsNotOrderByOrderDesc(folderId,0);
        return folders.stream().map(Folder::toDTO).collect(Collectors.toList());
    }

    public List<FileRequestDto> getFiles(String folderId){
        List<FileMogo> files =fileMogoRepository.findByFolderIdAndStatusIsNot(folderId,0);
        return files.stream()
                .map(FileMogo::toDto)
                .collect(Collectors.toList());
    }

    public FolderDto getParentFolder(String folderId){
        Optional<Folder> opt = folderMogoRepository.findById(folderId);
        if(opt.isPresent()){
            Folder folder = opt.get();
            FolderDto folderDto = folder.toDTO();
            return folderDto;
        }
        return null;
    }

    public FolderDto getRootFolder(String uid){
        Optional<Folder> opt = folderMogoRepository.findByTypeAndOwnerId("ROOT",uid);
        if(opt.isPresent()){
            Folder folder = opt.get();
            FolderDto folderDto = folder.toDTO();
            return folderDto;
        }
        return null;
    }

    public FolderDto updateFolder(String text, String newName){
        Optional<Folder> opt = folderMogoRepository.findById(text);
        FolderDto result = null;
        if(opt.isPresent()){
            Folder folder = opt.get();
            folder.newFolderName(newName);
            Folder savedFolder= folderMogoRepository.save(folder);
            result = savedFolder.toDTO();
        }

        return result;
    }


    public double updateFolder(MoveFolderRequest updateRequest) {
        // Optional을 사용하여 폴더를 조회
        Folder folder = folderMogoRepository.findById(updateRequest.getFolderId())
                .orElseThrow(() -> new RuntimeException("Folder not found with ID: " + updateRequest.getTargetFolderId()));

        // 폴더가 존재하면 order 업데이트
        folder.moveOrder(updateRequest.getOrder());

        // 변경된 폴더 저장
        Folder changedFolder =folderMogoRepository.save(folder);

        return changedFolder.getOrder();
    }



    public void moveToFolder(MoveFolderRequest request){
        Folder draggfolder = folderMogoRepository.findById(request.getFolderId()).orElseThrow(() -> new RuntimeException("Folder not found with ID: " + request.getFolderId()));
        Folder targetFolder = folderMogoRepository.findById(request.getTargetFolderId()).orElseThrow(() -> new RuntimeException("Folder not found with ID: " + request.getTargetFolderId()));

        log.info("draggfolderPath "+draggfolder.getPath());
        log.info("targetFolderPath "+targetFolder.getOrder());
        String newPath = targetFolder.getPath()+"/"+draggfolder.getFolderUUID();

        long orders  = folderMogoRepository.countByParentId(targetFolder.getId());
        double order =  (orders +1)*100.0;
        Query query = new Query(Criteria.where("_id").is(draggfolder.getId()));
        Update update = new Update().set("path",newPath ).set("updateAt",LocalDateTime.now()).set("parentId",targetFolder.getId()).set("order" , order);
        mongoTemplate.upsert(query, update, Folder.class);

        boolean updateSuccessful = updateFolderPaths(draggfolder, newPath);

        if (!updateSuccessful) {
            throw new RuntimeException("Failed to update paths for all subfolders and files.");
        }

        sftpService.moveToFolder(draggfolder.getPath(),newPath);

    }


    public boolean moveFileToFolder(MoveFolderRequest request){
        Folder targetFolder = folderMogoRepository.findById(request.getTargetFolderId()).orElseThrow(() -> new RuntimeException("Folder not found with ID: " + request.getTargetFolderId()));

        FileMogo file = fileMogoRepository.findById(request.getFileId()).orElseThrow(() -> new RuntimeException("File not found with ID: " + request.getFileId()));

        String newPath = targetFolder.getPath() + "/"+file.getSavedName();
        Query query = new Query(Criteria.where("_id").is(file.getId()));
        Update update = new Update()
                .set("path",newPath )
                .set("updateAt",LocalDateTime.now())
                .set("folderId",targetFolder.getId());
        mongoTemplate.upsert(query, update, FileMogo.class);
        return true;

    }

    public boolean updateFolderPaths(Folder parentFolder ,String newPath){

        List<FileMogo> fileMogos = fileMogoRepository.findAllByFolderId(parentFolder.getId());
        int size = fileMogos.size();
        int result = 0;
        for(FileMogo file : fileMogos){
            String fileNewPath = newPath+"/"+file.getSavedName();
            if (!fileNewPath.equals(file.getPath())) { // Check if update is needed
                file.updatePath(fileNewPath);
                file.updateDate();
                fileMogoRepository.save(file);
            }
            result++;
        }

        List<Folder> folders = folderMogoRepository.findAllByParentId(parentFolder.getId());
        size += folders.size();
        for(Folder folder : folders){
            String folderNewPath = newPath+"/"+folder.getFolderUUID();
            if (!folderNewPath.equals(folder.getPath())) { // Check if update is needed
                folder.setPath(folderNewPath);
                folder.updatedTime();
                folderMogoRepository.save(folder);

                updateFolderPaths(folder, folderNewPath);
            }
            result++;

        }

        if(size==result){
            return true;
        }else{
            return false;
        }

    }


    @Transactional
    public boolean uploadFiles(List<MultipartFile> files , String folderId,double maxOrder,String uid){
         boolean result = false;
         Optional<Folder> opt = folderMogoRepository.findById(folderId);

         String remoteDir = null;
         double savedOrder = 0;
         int isShared = 0;
         int isPinned = 0;
         if(opt.isPresent()){
             Folder folder = opt.get();
             remoteDir = folder.getPath();
             isShared = folder.getIsShared();
             isPinned = folder.getIsPinned();
         }
         int size =0 ;
        for(MultipartFile file : files){

            String originalFilename = file.getOriginalFilename();
            String savedFilename= generateSavedName(originalFilename);
            String path = remoteDir+"/"+savedFilename;

            savedOrder = maxOrder == 0 ? 0 : maxOrder+100.0;

            FileRequestDto filedto= FileRequestDto.builder()
                    .folderId(folderId)
                    .savedName(savedFilename)
                    .originalName(originalFilename)
                    .path(path)
                    .file_order(savedOrder)
                    .isPinned(isPinned)
                    .isShared(isShared)
                    .ownerUid(uid)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .size(file.getSize())
                    .status(1)
                    .build();

            FileMogo saved = filedto.toEntity();

            // 임시 파일 생성
            File tempFile = null;
            try {
                tempFile = File.createTempFile("upload_", "_" + originalFilename);
                file.transferTo(tempFile); // MultipartFile 데이터를 임시 파일로 저장

                // SFTP 업로드
               String remoteFilePath =  sftpService.uploadFile(tempFile.getAbsolutePath(), remoteDir, savedFilename);
               String thumbnailPath = thumbnailService.generateThumbnailIfNotExists(remoteFilePath,savedFilename);


                // 업로드된 파일 정보 저장
                fileMogoRepository.save(saved);
            } catch ( IOException e) {
                log.error("임시 파일 생성 또는 전송 중 오류 발생: {}", e.getMessage());
            } finally {
                if (tempFile != null && tempFile.exists()) {
                    if (tempFile.delete()) {
                        log.info("임시 파일 삭제 성공: {}", tempFile.getAbsolutePath());
                    } else {
                        log.warn("임시 파일 삭제 실패: {}", tempFile.getAbsolutePath());
                    }
                }
            }
           FileMogo savedFile =  fileMogoRepository.save(saved);
            size ++;
        }

        if(size == files.size()){
            return true;
        }else{
            return false;
        }



    }

    // 파일 저장 로직
    public boolean saveFileToFolder(MultipartFile file, String folderId, double fileOrder, String uid) {
        try {
            List<MultipartFile> files = Collections.singletonList(file);
            return uploadFiles(files, folderId, fileOrder, uid);
        } catch (Exception e) {
            log.error("파일 저장 중 오류 발생: {}", e.getMessage(), e);
            return false; // 실패 시 false 반환
        }
    }







    public String generateSavedName(String originalName) {
        // Validate input
        if (originalName == null || originalName.isEmpty()) {
            throw new IllegalArgumentException("Original file name cannot be null or empty");
        }

        // Extract file extension
        String extension = "";
        int dotIndex = originalName.lastIndexOf(".");
        if (dotIndex > 0 && dotIndex < originalName.length() - 1) {
            extension = originalName.substring(dotIndex);
        }

        // Generate UUID and append extension
        String uuid = UUID.randomUUID().toString();
        return uuid + extension;
    }


    public void reNameFolder(RenameRequest renameRequest) {
        Folder folder = folderMogoRepository.findById(renameRequest.getId()).orElseThrow();

        if(folder !=null){
            Query query = new Query(Criteria.where("_id").is(renameRequest.getId()));
            Update update = new Update()
                    .set("name", renameRequest.getNewName());
            mongoTemplate.upsert(query, update, Folder.class);
        }

    }

    public void reNameFile(String id, String newName){

        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update().set("originalName", newName);

        mongoTemplate.upsert(query, update, FileMogo.class);

    }

    public Map<String ,String > goToTrash(String id, String type,String currentUser,String permission){
        Map<String ,String > result = new HashMap<>();
            if(type.equals("folder")){
                Folder folder = folderMogoRepository.findById(id).orElseThrow();
                if(folder.getOwnerId().equals(currentUser) || permission.equals("모든")){
                    Query query = new Query(Criteria.where("_id").is(id));
                    Update update = new Update()
                            .set("status", 0).set("target",1).set("updateAt", LocalDateTime.now());
                    mongoTemplate.upsert(query, update, Folder.class);
                    updateAllChildren(id);
                    result.put("result","success");
                     result.put("message","휴지통 이동 성공");


                }else{
                    result.put("result","fail");
                    result.put("message","삭제 권한이 없습니다.");
                }

            }else if(type.equals("file")) {
                FileMogo fileMogo = fileMogoRepository.findById(id).orElseThrow();
                if (fileMogo.getOwnerUid().equals(currentUser) || permission.equals("모든")) {
                    Query query = new Query(Criteria.where("_id").is(id));
                    Update update = new Update().set("status", 0);

                    mongoTemplate.upsert(query, update, FileMogo.class);
                    result.put("result","success");
                    result.put("message","휴지통 이동 성공");
                } else {
                    result.put("result","fail");
                    result.put("message","삭제 권한이 없습니다.");
                }

            }

            return result;



    }

    public void updateAllChildren(String parentId) {
        // 현재 폴더의 하위 폴더들을 조회
        Query folderQuery = new Query(Criteria.where("parentId").is(parentId));
        List<Folder> childFolders = mongoTemplate.find(folderQuery, Folder.class);
        Update update = new Update().set("status", 0);


        // 하위 폴더 업데이트
        mongoTemplate.updateMulti(folderQuery, update, Folder.class);

        // 하위 파일 업데이트
        Query fileQuery = new Query(Criteria.where("parentId").is(parentId));
        mongoTemplate.updateMulti(fileQuery, update, FileMogo.class);

        // 하위 폴더들의 ID 가져오기

        // 각 하위 폴더에 대해 재귀적으로 처리
        for (Folder childFolder : childFolders) {
            updateAllChildren(childFolder.getId());
        }
    }



    //zip파일 생성
    public String makeZipfolder(String folderId){
        log.info("folderID "+folderId);
        Optional<Folder> opt = folderMogoRepository.findById(folderId);

        if(opt.isPresent()){
            Folder folder = opt.get();
            log.info("folder "+folder);

            boolean result = sftpService.makeZip(folder.getPath(), folder.getName());
            if(result){
                return folder.getName()+".zip";
            }

            return null;
        }
        return null;
    }


    public int  favorite(String id,String type){
        int savedPinned = 0;

        if(type.equals("folder")){
            Folder folder = folderMogoRepository.findById(id).orElseThrow();
            int isPinned = folder.getIsPinned();
            if(isPinned == 0) {
                savedPinned = 1;
            }
            Query query = new Query(Criteria.where("_id").is(id));
            Update update = new Update()
                    .set("isPinned", savedPinned)
                    .set("updateAt", LocalDateTime.now());
            mongoTemplate.upsert(query, update, Folder.class);
        }else if(type.equals("file")){
            FileMogo fileMogo = fileMogoRepository.findById(id).orElseThrow();
            int isPinned = fileMogo.getIsPinned();
            if(isPinned == 0) {
                savedPinned = 1;
            }
            Query query = new Query(Criteria.where("_id").is(id));
            Update update = new Update()
                    .set("isPinned", savedPinned)
                    .set("updateAt", LocalDateTime.now());

            mongoTemplate.upsert(query, update, FileMogo.class);
        }

        return savedPinned;
    }

    //조아요 목록
    public List<FolderDto> isFavorite(String uid){
        List<Folder> folders = folderMogoRepository.findByOwnerIdAndIsPinnedAndStatus(uid,1,1);

        List<FolderDto> folderDtos = folders.stream().map(Folder::toDTO).collect(Collectors.toList());

        return folderDtos;
    }

    public List<FileRequestDto> isFavoriteFile(String uid){

        List<FileMogo> files = fileMogoRepository.findByOwnerUidAndIsPinnedAndStatusIsNot(uid,1,0);

        return files.stream()
                .map(FileMogo::toDto)
                .collect(Collectors.toList());

    }

    //최근문서
    public List<FolderDto> latestFolder(String uid){
        List<Folder> folders = folderMogoRepository.findByOwnerIdAndParentIdIsNotNullAndStatusIsNotOrderByUpdatedAtDesc(uid,0);

        List<FolderDto> folderDtos = folders.stream().map(Folder::toDTO).collect(Collectors.toList());

        return folderDtos;
    }

    public List<FileRequestDto> latestFile(String uid){

        List<FileMogo> files = fileMogoRepository.findByOwnerUidAndStatusIsNotOrderByUpdatedAtDesc(uid,0);

        return files.stream()
                .map(FileMogo::toDto)
                .collect(Collectors.toList());

    }


    //휴지통
    public List<FolderDto> trashFolder(String uid){
        List<Folder> folders = folderMogoRepository.findByOwnerIdAndTargetAndStatus(uid,1,0);

        List<FolderDto> folderDtos = folders.stream().map(Folder::toDTO).collect(Collectors.toList());

        return folderDtos;
    }

    public List<FileRequestDto> trashFile(String uid){

        List<FileMogo> files = fileMogoRepository.findByOwnerUidAndStatus(uid,0);

        return files.stream()
                .map(FileMogo::toDto)
                .collect(Collectors.toList());

    }

    public boolean restore(String type,String id){
        log.info("여기 들어온다 복구");

        try{
            UpdateResult updateResult = null;
            if(type.equals("folder")){
                Folder folder = folderMogoRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Folder not found with ID: " + id));

                Query query = new Query(Criteria.where("_id").is(id));
                Update update = new Update().set("status", 1).set("updatedAt", new Date());
                updateResult =  mongoTemplate.upsert(query, update, Folder.class);
                updateRestoreAllChildren(id);


            }else if(type.equals("file")){
                FileMogo file = fileMogoRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("File not found with ID: " + id));
                Query query = new Query(Criteria.where("_id").is(id));
                Update update = new Update().set("status", 1).set("updatedAt", new Date());
                updateResult = mongoTemplate.upsert(query, update, FileMogo.class);
            } else {
                throw new IllegalArgumentException("Invalid type specified: " + type);
            }


            return updateResult != null && updateResult.getModifiedCount()>0;
        }catch (IllegalArgumentException e){
            // 처리할 수 없는 입력 값에 대한 에러 로그
            log.error("Invalid input for restore: type={}, id={}", type, id, e);
            return false;
        }catch (Exception e) {
            // 데이터베이스 작업 중 발생한 일반적인 예외 처리
            log.error("Error occurred while restoring: type={}, id={}", type, id, e);
            return false;
        }

    }

    public void updateRestoreAllChildren(String parentId) {
        // 현재 폴더의 하위 폴더들을 조회
        Query folderQuery = new Query(Criteria.where("parentId").is(parentId));
        Update update = new Update().set("status", 1).set("updatedAt", new Date());

        // 하위 폴더 업데이트
        mongoTemplate.updateMulti(folderQuery, update, Folder.class);

        // 하위 파일 업데이트
        Query fileQuery = new Query(Criteria.where("parentId").is(parentId));
        mongoTemplate.updateMulti(fileQuery, update, FileMogo.class);

        // 하위 폴더들의 ID 가져오기
        List<Folder> childFolders = mongoTemplate.find(folderQuery, Folder.class);

        // 각 하위 폴더에 대해 재귀적으로 처리
        for (Folder childFolder : childFolders) {
            updateAllChildren(childFolder.getId());
        }
    }

    private String getUserRootPath(String userId) {
        return "uploads/" + userId;
    }
    // Example restore logic handling this scenario
    @Transactional
    public boolean restoreFolder(String folderId,String currentUser) {
        log.info("[START] 폴더 복구 시작: folderId={}", folderId);

        // 복구할 폴더 조회
        Folder folder = folderMogoRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found with ID: " + folderId));
        log.info("복구 대상 폴더 조회 완료: {}", folder);

        // 사용자 ROOT 폴더 경로 확인

        String userRootPath = getUserRootPath(folder.getOwnerId());
        log.info("사용자 ROOT 폴더 경로: {}", userRootPath);


        Folder rootFolder = folderMogoRepository.findByPath(userRootPath)
                .orElseThrow(() -> new IllegalArgumentException("Root folder not found"));
        log.info("ROOT 폴더 조회 완료: {}", rootFolder);

        List<Restore> restoreLogs  = restoreRepository.findByOriginalFolderIdAndStatusOrderByRestoreDateDesc(folderId,1);
        if(restoreLogs.size() >0 || !restoreLogs.isEmpty()){
            log.info("restore로그 발견!!!! "+restoreLogs);
            return  applyRestoreLog(restoreLogs.get(0));
        }
        Folder restoreFolder  = restoreParentFolders(folder.getParentId(), rootFolder,currentUser);
        String restoredPath = restoreFolder.getPath();
        log.info("복구된 상위 폴더 경로: {}", restoredPath);

        if(restoreFolder.getStatus() != 0 ){
            // MongoDB 업데이트
            Query query = new Query(Criteria.where("_id").is(folderId));
            Update update = new Update()
                    .set("status", 1)
                    .set("restore", 0)
                    .set("target",0)
                    .set("updatedAt", new Date());
            mongoTemplate.upsert(query, update, Folder.class);

            updateRestoreAllChildren(folderId);
            return true;
        }

        // SFTP 복구 호출
        boolean sftpRestoreSuccess = sftpService.restoreMoveFolder(folder.getPath(), restoredPath);
        if (!sftpRestoreSuccess) {
            log.error("SFTP 폴더 복구 실패: oldPath={} newPath={}", folder.getPath(), restoredPath);
            return false;
        }
        log.info("SFTP 폴더 복구 성공: oldPath={} newPath={}", folder.getPath(), restoredPath);

        // 새로운 parentId 설정 (복구된 상위 폴더 ID)
        String newParentFolderId =restoreFolder.getId();
        log.info("복구된 상위 폴더 ID: {}", newParentFolderId);

        // MongoDB 업데이트
        Query query = new Query(Criteria.where("_id").is(folderId));
        Update update = new Update()
                .set("status", 1)
                .set("parentId", newParentFolderId)
                .set("path", restoredPath + "/" + folder.getFolderUUID())
                .set("restore", 1)
                .set("target",0)
                .set("updatedAt", new Date());
        mongoTemplate.upsert(query, update, Folder.class);
        log.info("MongoDB 폴더 업데이트 완료: folderId={}", folderId);

        // 하위 파일들의 folderId와 path 업데이트
        folder.setPath(restoredPath + "/" + folder.getFolderUUID());
        folder.updateParentId(restoreFolder.getId());
        boolean updateFile = updateFilePaths(folder);
        log.info("하위 파일 경로 업데이트 결과: {}", updateFile);

        boolean updateChildrenFolder = restoreChildFolders(folder,newParentFolderId);
        log.info("하위 폴더 복구 결과: {}", updateChildrenFolder);

        if (updateFile && updateChildrenFolder) {
            log.info("[END] 폴더 복구 완료: folderId={}", folderId);
            return true;
        } else {
            log.warn("[END] 폴더 복구 실패: folderId={}", folderId);
            return false;
        }
    }


    @Transactional
    public boolean applyRestoreLog(Restore restoreLog) {
        log.info("[START] RESTORE 로그 기반 복구 작업 시작: {}", restoreLog);

        try {
            // 기존 경로와 복구 경로 가져오기
            String originalPath = restoreLog.getOriginalPath();
            String restorePath = restoreLog.getRestorePath();
            String restoreFolderId = restoreLog.getRestoreFolderId();

            // SFTP로 폴더 내용 덮어쓰기
            if (!sftpService.copyFolderContents(originalPath, restorePath)) {
                log.error("SFTP 복사 실패: originalPath={}, restorePath={}", originalPath, restorePath);
                return false;
            }
            log.info("SFTP 복사 완료: originalPath={}, restorePath={}", originalPath, restorePath);

            // MongoDB에서 `originalPath`를 가진 하위 폴더와 파일들 조회
            List<Folder> childFolders = folderMogoRepository.findAllByPathStartingWith(originalPath);
            List<FileMogo> childFiles = fileMogoRepository.findAllByPathStartingWith(originalPath);

            log.info("하위 폴더 개수: {}, 하위 파일 개수: {}", childFolders.size(), childFiles.size());

            // 하위 폴더 경로와 부모 ID 업데이트
            for (Folder childFolder : childFolders) {
                String newFolderPath = restorePath + childFolder.getPath().substring(originalPath.length());
                Query folderQuery = new Query(Criteria.where("_id").is(childFolder.getId()));
                Update folderUpdate = new Update()
                        .set("path", newFolderPath)
                        .set("parentId", restoreFolderId)
                        .set("status", 1)
                        .set("restore", 0)
                        .set("updatedAt", LocalDateTime.now());
                mongoTemplate.updateFirst(folderQuery, folderUpdate, Folder.class);
            }

            // 하위 파일 경로 업데이트
            for (FileMogo childFile : childFiles) {
                String newFilePath = restorePath + "/"+childFile.getSavedName();
                Query fileQuery = new Query(Criteria.where("_id").is(childFile.getId()));
                Update fileUpdate = new Update()
                        .set("path", newFilePath)
                        .set("folderId",restoreFolderId)
                        .set("status", 1)
                        .set("updatedAt", LocalDateTime.now());
                mongoTemplate.updateFirst(fileQuery, fileUpdate, FileMogo.class);
            }

            log.info("하위 폴더와 파일 경로 업데이트 완료");

            // MongoDB에서 복구된 폴더의 경로와 상태 업데이트
            if (!updateFolderAndFilesPaths(restoreFolderId, restorePath)) {
                log.error("MongoDB 경로 업데이트 실패: restoreFolderId={}", restoreFolderId);
                return false;
            }
            log.info("MongoDB 경로 업데이트 완료: restoreFolderId={}", restoreFolderId);

            // 복구 완료 로그 업데이트
            restoreLog.setConflictHandled("1");
            restoreLog.setStatus(0);
            restoreRepository.save(restoreLog);

            log.info("[END] RESTORE 로그 기반 복구 작업 완료: {}", restoreLog);
            deleteOriginalChildrenByPath(restoreLog.getOriginalPath());

            //기존 DB 삭제되어야 하는데?
            folderMogoRepository.deleteById(restoreLog.getOriginalFolderId());
            sftpService.delete(originalPath);
            return true;
        } catch (Exception e) {
            log.error("RESTORE 로그 기반 복구 작업 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }

    private void deleteOriginalChildrenByPath(String parentPath) {
        log.info("[START] 경로 기반 하위 폴더 및 파일 삭제: parentPath={}", parentPath);

        // 하위 폴더 삭제
        Query folderQuery = new Query(Criteria.where("path").regex("^" + parentPath));
        List<Folder> folders = mongoTemplate.find(folderQuery, Folder.class);
        for (Folder folder : folders) {
            mongoTemplate.remove(new Query(Criteria.where("_id").is(folder.getId())), Folder.class);
            log.info("삭제된 폴더: {}", folder);
        }
    }


    @Transactional
    public boolean updateFolderAndFilesPaths(String folderId, String newPath) {
        log.info("[START] MongoDB 경로 업데이트: folderId={}, newPath={}", folderId, newPath);

        List<Restore> restoreLogs  = restoreRepository.findByOriginalFolderIdAndStatusOrderByRestoreDateDesc(folderId,1);

        if(restoreLogs.size()>0 || !restoreLogs.isEmpty()){
            for (Restore restoreLog : restoreLogs) {
                String originalPath = restoreLog.getOriginalPath();
                folderMogoRepository.deleteById(restoreLog.getOriginalFolderId());
                log.info("기존 경로 데이터 삭제 완료: {}", originalPath);
            }
        }

        // 상위 폴더 경로 업데이트
        Query folderQuery = new Query(Criteria.where("_id").is(folderId));
        Update folderUpdate = new Update()
                .set("path", newPath)
                .set("target",0)
                .set("status", 1)
                .set("restore",0)
                .set("updatedAt", LocalDateTime.now());
        mongoTemplate.updateFirst(folderQuery, folderUpdate, Folder.class);
        log.info("폴더 경로 업데이트 완료: folderId={}", folderId);

        // 하위 파일 경로 업데이트
        Query fileQuery = new Query(Criteria.where("folderId").is(folderId));
        List<FileMogo> files = mongoTemplate.find(fileQuery, FileMogo.class);
        int updatedFilesCount = 0;

        for (FileMogo file : files) {
            String newFilePath = newPath + "/" + file.getSavedName();
            Query updateQuery = new Query(Criteria.where("_id").is(file.getId()));
            Update fileUpdate = new Update()
                    .set("path", newFilePath)
                    .set("status", 1)
                    .set("updatedAt", LocalDateTime.now());
            mongoTemplate.updateFirst(updateQuery, fileUpdate, FileMogo.class);
            updatedFilesCount++;
        }
        log.info("하위 파일 경로 업데이트 완료: updatedFilesCount={}", updatedFilesCount);

        // 하위 폴더에 대해 재귀적으로 처리
        List<Folder> childFolders = folderMogoRepository.findAllByParentId(folderId);
        for (Folder childFolder : childFolders) {
            String childNewPath = newPath + "/" + childFolder.getFolderUUID();
            updateFolderAndFilesPaths(childFolder.getId(), childNewPath);
        }

        log.info("[END] MongoDB 경로 업데이트 완료: folderId={}, newPath={}", folderId, newPath);
        return true;
    }




    @Transactional
    public Folder restoreParentFolders(String parentId, Folder rootFolder,String currentUser) {
        log.info("[START] 상위 폴더 복구: parentId={}", parentId);
        if (parentId.equals(rootFolder.getId())) {
            log.info("ROOT 폴더 경로 반환: {}", rootFolder.getPath());
            return rootFolder; // ROOT 폴더 경로 반환
        }

        // 상위 폴더 조회
        Folder parentFolder = folderMogoRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Deleted parent folder not found: " + parentId));

        log.info("상위 폴더 조회 완료: {}", parentFolder);


//        // 복구된 상위 폴더 존재 확인
//        Folder restoredFolder = folderMogoRepository.findFolderByNameAndParentIdAndRestore(
//                parentFolder.getName(), parentFolder.getParentId(), 1);
//
//        if (restoredFolder != null) {
//            log.info("이미 복구된 상위 폴더 발견: {}", restoredFolder);
//
//            // 기존 상위 폴더의 내용을 새 경로로 복사
//            String restoredPath = restoredFolder.getPath();
//            boolean copySuccess = sftpService.copyFolderContents(parentFolder.getPath(), restoredPath);
//            if (!copySuccess) {
//                log.error("SFTP 내용 복사 실패: source={} target={}", parentFolder.getPath(), restoredPath);
//                throw new RuntimeException("SFTP 내용 복사 실패");
//            }
//
//            // MongoDB 경로 및 상태 갱신
//            Query query = new Query(Criteria.where("_id").is(parentFolder.getId()));
//            Update update = new Update()
//                    .set("path", restoredPath)
//                    .set("status", 1)
//                    .set("restore", 1)
//                    .set("updatedAt", new Date());
//            mongoTemplate.upsert(query, update, Folder.class);
//            restoredFolder.setPath(restoredPath);
//            log.info("MongoDB 폴더 업데이트 완료: {}", parentFolder.getId());
//            return restoredFolder; // 복구된 폴더 경로 반환
//        }


        if (parentFolder.getStatus() == 0) {
            log.info("상위 폴더가 삭제된 상태. 복구를 진행합니다.");

            // 부모 폴더 복구
            Folder parentRestore =  restoreParentFolders(parentFolder.getParentId(), rootFolder,currentUser);
            String parentRestoredPath = parentRestore.getPath();
            String newFolderUUID = UUID.randomUUID().toString();
            log.info("새로운 폴더 UUID 생성: {}", newFolderUUID);

            // 상위 폴더 SFTP 생성
            String newParentPath = parentRestoredPath + "/" + newFolderUUID;
            log.info("상위 폴더 SFTP 생성 경로: {}", newParentPath);
            boolean parentFolderCreated = sftpService.RestoreCreateFolder(newParentPath);
            if (!parentFolderCreated) {
                log.error("SFTP 상위 폴더 생성 실패: path={}", newParentPath);
                throw new RuntimeException("SFTP 상위 폴더 생성 실패: path=" + newParentPath);
            }

            // MongoDB에 상위 폴더 추가
            Folder restoredParentFolder = Folder.builder()
                    .parentId(parentRestore.getId())
                    .folderUUID(newFolderUUID)
                    .description(parentFolder.getDescription())
                    .invitations(parentFolder.getInvitations())
                    .name(parentFolder.getName())
                    .path(newParentPath)
                    .ownerId(parentFolder.getOwnerId())
                    .type(parentFolder.getType())
                    .order(parentFolder.getOrder())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .isPinned(parentFolder.getIsPinned())
                    .isShared(parentFolder.getIsShared())
                    .sharedDepts(parentFolder.getSharedDepts())
                    .sharedUsers(parentFolder.getSharedUsers())
                    .linkSharing(parentFolder.getLinkSharing())
                    .status(1) // 활성화 상태
                    .restore(1) // 복구 플래그
                    .build();

            Folder savedFolder = folderMogoRepository.save(restoredParentFolder);
            log.info("상위 폴더 MongoDB 복구 완료: {}", restoredParentFolder);
            saveRestoreLog(parentFolder.getId(), savedFolder.getId(), newParentPath, parentFolder.getPath(),currentUser, 1);

            return savedFolder;
        }else {

            return  parentFolder;
        }

    }

    // 복구 이력 저장 메서드
    private void saveRestoreLog(String originalFolderId, String restoredFolderId, String restoredPath,String originalPath,String currentUser, int status) {

        Restore restore = Restore.builder()
                .restoreBy(currentUser)
                .restoreFolderId(restoredFolderId)
                .originalPath(originalPath)
                .originalFolderId(originalFolderId)
                .restorePath(restoredPath)
                .restoreDate(LocalDateTime.now())
                .status(status)
                .conflictHandled("0")
                .build();
        restoreRepository.save(restore);
    }

    @Transactional
    public boolean updateFilePaths(Folder folder) {
        log.info("[START] 하위 파일 경로 업데이트: folderId={}", folder.getId());

        List<FileMogo> files = fileMogoRepository.findAllByFolderId(folder.getId());
        log.info("폴더 내 파일 개수: {}", files.size());
        String newFolderPath = folder.getPath();

        int size = files.size();
        int result = 0;

        for (FileMogo file : files) {
            String newFilePath = newFolderPath + "/" + file.getSavedName();
            log.info("파일 경로 업데이트: oldPath={}, newPath={}", file.getPath(), newFilePath);

            Query query = new Query(Criteria.where("_id").is(file.getId()));
            Update update = new Update()
                    .set("path", newFilePath)
                    .set("updatedAt", new Date());
            mongoTemplate.upsert(query, update, FileMogo.class);

            result++;
        }

        log.info("[END] 파일 경로 업데이트 완료. 성공/전체: {}/{}", result, size);
        return result == size;
    }

    @Transactional
    public boolean restoreChildFolders(Folder parentFolder, String newParentFolderId) {
        log.info("[START] 하위 폴더 복구: parentFolderId={}, newParentFolderId={}", parentFolder.getId(), newParentFolderId);

        List<Folder> childFolders = folderMogoRepository.findAllByParentId(parentFolder.getId());
        log.info("하위 폴더 개수: {}", childFolders.size());

        String parentFolderPath = parentFolder.getPath();
        int totalChildren = childFolders.size();
        int successfulRestorations = 0;

        for (Folder childFolder : childFolders) {
            // 새로운 경로 생성
            String newFolderPath = parentFolderPath + "/" + childFolder.getFolderUUID();
            log.info("하위 폴더 경로 업데이트: oldPath={}, newPath={}", childFolder.getPath(), newFolderPath);

            // MongoDB에서 하위 폴더의 `path`와 `parentId` 업데이트
            Query query = new Query(Criteria.where("_id").is(childFolder.getId()));
            Update update = new Update()
                    .set("parentId", parentFolder.getId()) // 상위 폴더의 새로운 ID 설정
                    .set("path", newFolderPath)
                    .set("status", 1)
                    .set("restore", 1)
                    .set("updatedAt", new Date());
            mongoTemplate.upsert(query, update, Folder.class);

            // 하위 폴더의 새로운 데이터 적용
            childFolder.setPath(newFolderPath);
            childFolder.updateParentId(parentFolder.getId());

            // 하위 폴더에 속한 파일의 경로 업데이트
            boolean filesUpdated = updateFilePaths(childFolder);
            log.info("하위 파일 경로 업데이트 결과: childFolderId={}, filesUpdated={}", childFolder.getId(), filesUpdated);

            // 재귀적으로 하위 폴더 복구 호출
            boolean childRestored = restoreChildFolders(childFolder, childFolder.getId());
            log.info("재귀적 하위 폴더 복구 결과: childFolderId={}, childRestored={}", childFolder.getId(), childRestored);

            if (filesUpdated && childRestored) {
                successfulRestorations++;
            }
        }

        log.info("[END] 하위 폴더 복구 완료: 성공/전체={}/{}", successfulRestorations, totalChildren);
        return successfulRestorations == totalChildren;
    }


    //진짜 삭제
    @Transactional
    public boolean delete(String type,String id) {

        if(type.equals("folder")){
            Folder folder = folderMogoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Folder not found with ID: " + id));
            deleteFolderRecursively(folder);
            String path = folder.getPath();
            folderMogoRepository.deleteById(folder.getId());
            boolean result = sftpService.delete(path);
            return true;
        }else if(type.equals("file")){
            FileMogo fileMogo = fileMogoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("File not found with ID: " + id));
            String path = fileMogo.getPath();
            fileMogoRepository.deleteById(fileMogo.getId());
            boolean result = sftpService.delete(path);
            sftpService.thumbnailDelete(fileMogo.getSavedName());
            return true;
        }


        return false;
    }


    // 재귀적으로 폴더와 파일 삭제
    private void deleteFolderRecursively(Folder folder) {
        // 하위 파일 삭제
        List<FileMogo> files = fileMogoRepository.findAllByFolderId(folder.getId());
        for (FileMogo file : files) {
            String filePath = file.getPath();
            fileMogoRepository.deleteById(file.getId()); // DB에서 파일 삭제
            sftpService.delete(filePath); // SFTP에서 파일 삭제
        }

        // 하위 폴더 삭제
        List<Folder> subFolders = folderMogoRepository.findAllByParentId(folder.getId());
        for (Folder subFolder : subFolders) {
            folderMogoRepository.deleteById(subFolder.getId());
            deleteFolderRecursively(subFolder); // 하위 폴더에 대해 재귀 호출
        }
    }

    public void seletedDeleted(DeletedRequest deletedRequest){
        if (deletedRequest == null) {
            throw new IllegalArgumentException("DeletedRequest cannot be null");
        }
        List<String> folders = deletedRequest.getFolders();
        if (folders != null && !folders.isEmpty()) {
            for (String folder : folders) {
                if (folder == null || folder.isBlank()) {
                    throw new IllegalArgumentException("Folder ID cannot be null or blank");
                }
                delete("folder", folder);
            }
        }
        List<String> files = deletedRequest.getFiles();
        if (files != null && !files.isEmpty()) {
            for (String file : files) {
                if (file == null || file.isBlank()) {
                    throw new IllegalArgumentException("File ID cannot be null or blank");
                }
                delete("file", file);
            }
        }


    }

    public boolean deleteAll( DeletedRequest deletedRequest){
        int result = 0;
        int size = 0;
        if(!deletedRequest.getFileDtos().isEmpty()){
            List<FileRequestDto> files = deletedRequest.getFileDtos();
            size += files.size();
                for (FileRequestDto file : files) {
                    if (file == null || file.getId().isEmpty()) {
                        throw new IllegalArgumentException("File ID cannot be null or blank");
                    }
                    delete("file", file.getId());
                    result++;
                }
        }
        if(!deletedRequest.getSubFolders().isEmpty()){
            List<FolderDto> subFolders = deletedRequest.getSubFolders();
            size += subFolders.size();
            if (subFolders != null && !subFolders.isEmpty()) {
                for (FolderDto folder : subFolders) {
                    if (folder == null || folder.getId().isBlank()) {
                        throw new IllegalArgumentException("Folder ID cannot be null or blank");
                    }
                    delete("folder",  folder.getId());
                    result++;
                }
            }

        }

        if(size == result){
            return true;
        }else{
            return false;
        }




    }













}
