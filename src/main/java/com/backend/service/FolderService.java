package com.backend.service;


import com.backend.dto.request.FileRequestDto;
import com.backend.dto.request.drive.DeletedRequest;
import com.backend.dto.request.drive.MoveFolderRequest;
import com.backend.dto.request.drive.NewDriveRequest;
import com.backend.dto.request.drive.RenameRequest;
import com.backend.dto.response.drive.FolderDto;
import com.backend.document.drive.FileMogo;
import com.backend.document.drive.Folder;
import com.backend.dto.response.drive.NewNameResponseDto;
import com.backend.repository.drive.FileMogoRepository;
import com.backend.repository.drive.FolderMogoRepository;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.internal.bulk.UpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
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
    private String fileServerUrl = "http://43.202.45.49:90/local/upload/create-folder";


    @Value("${spring.servlet.multipart.location}")
    private String uploadPath;


    public String createFolder(NewDriveRequest request){
        String uid = request.getOwner();
        NewNameResponseDto makeDrive = null;

        if(request.getParentFolder() != null){
            FolderDto folderDto = request.getParentFolder();
            Folder exisitingFolder = folderMogoRepository.findFolderByNameAndParentId(request.getName(),folderDto.getId());
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
                   .order(request.getOrder()==0 ? 0 : (request.getOrder()+1.0)*100) // 널 체크
                   .parentId(request.getParentId())
                   .path(makeDrive.getPath())
                   .folderUUID(makeDrive.getFolderUUID())
                   .type(request.getType())
                   .ownerId(uid)
                   .description(request.getDescription())
                   .status(1)
                   .isShared(request.getIsShared())
                   .linkSharing(request.getLinkSharing())
                   .updatedAt(LocalDateTime.now())
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
                        .status(folder.getStatus())
                        .linkSharing(folder.getLinkSharing())
                        .updatedAt(folder.getUpdatedAt())
                        .parentId(parentId)
                        .build();
                log.info(folderDto.toString());
                return folderDto;
            }).collect(Collectors.toList());
            return folderDtos;


    }


    public Folder getFolderName(String uid){
        return folderMogoRepository.findByName(uid);
    }
    public Folder existFolder(String name,String parentId){
        return folderMogoRepository.findFolderByNameAndParentId(name,parentId);
    }

    public List<FolderDto> getSubFolders(String ownerId, String folderId){
        List<Folder> folders =folderMogoRepository.findByOwnerIdAndParentIdAndStatusIsNotOrderByOrder(ownerId,folderId,0);


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

    public boolean goToTrash(String id, String type){
        if(type.equals("folder")){
            Query query = new Query(Criteria.where("_id").is(id));
            Update update = new Update().set("status", 0);

            mongoTemplate.upsert(query, update, Folder.class);
            return true;
        }else if(type.equals("file")){
            Query query = new Query(Criteria.where("_id").is(id));
            Update update = new Update().set("status", 0);

            mongoTemplate.upsert(query, update, FileMogo.class);
            return true;
        }
        return false;
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
        List<Folder> folders = folderMogoRepository.findByOwnerIdAndStatus(uid,0);

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







}
