package com.backend.service;


import com.backend.dto.request.FileRequestDto;
import com.backend.dto.request.drive.MoveFolderRequest;
import com.backend.dto.request.drive.NewDriveRequest;
import com.backend.dto.request.drive.RenameRequest;
import com.backend.dto.response.drive.FolderDto;
import com.backend.document.drive.FileMogo;
import com.backend.document.drive.Folder;
import com.backend.dto.response.drive.NewNameResponseDto;
import com.backend.repository.drive.FileMogoRepository;
import com.backend.repository.drive.FolderMogoRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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



    public void uploadFiles(List<MultipartFile> files , String folderId,double maxOrder,String uid){

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
                    tempFile.delete(); // 임시 파일 삭제
                }
            }
           FileMogo savedFile =  fileMogoRepository.save(saved);


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
        Update update = new Update().set("name", newName);

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

    //진짜 삭제
    @Transactional
    public boolean deleteFolder(String id,String path,String type) {

        boolean result = sftpService.delete(path);
        if(result){
            if(type.equals("folder")){
                folderMogoRepository.deleteById(id);
                return true;
            }else if(type.equals("file")){
                fileMogoRepository.deleteById(id);
                return true;
            }

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


    public int  favorite(String id){
        Folder folder = folderMogoRepository.findById(id).orElseThrow();
        int isPinned = folder.getIsPinned();
        int savedPinned = 0;
        if(isPinned == 0) {
            savedPinned = 1;
        }
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update()
                .set("isPinned", savedPinned);
        mongoTemplate.upsert(query, update, Folder.class);
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
        List<Folder> folders = folderMogoRepository.findByOwnerIdAndStatusIsNotOrderByUpdatedAtDesc(uid,0);

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



}
