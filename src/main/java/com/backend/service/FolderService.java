package com.backend.service;


import com.backend.dto.request.FileRequestDto;
import com.backend.dto.request.drive.MoveFolderRequest;
import com.backend.dto.request.drive.NewDriveRequest;
import com.backend.dto.response.drive.FolderDto;
import com.backend.entity.folder.FileMogo;
import com.backend.entity.folder.Folder;
import com.backend.repository.drive.FileMogoRepository;
import com.backend.repository.drive.FolderMogoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
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
    private String fileServerUrl = "http://43.202.45.49:90/local/upload/create-folder";


    @Value("${spring.servlet.multipart.location}")
    private String uploadPath;


    public String createDrive(NewDriveRequest request){
        String uid = request.getOwner();
        String makeDrivePath = null;
        if(request.getParentFolder() !=null){
            FolderDto folderDto = request.getParentFolder();
           makeDrivePath = sftpService.createNewFolder(request.getName(), folderDto.getPath());

        }else{
            makeDrivePath = sftpService.createFolder(request.getName(),uid);

        }

        log.info("결과!!!!"+makeDrivePath);

        if(makeDrivePath != null){
           Folder folder =  Folder.builder()
                   .name(request.getName())
                   .order(request.getOrder() != 0.0 ? request.getOrder() : 0.0) // 널 체크
                   .parentId(request.getParentId())
                   .path(makeDrivePath)
                   .ownerId(uid)
                   .description(request.getDescription())
                   .status(0)
                   .isShared(request.getIsShared())
                   .linkSharing(request.getLinkSharing())
                   .updatedAt(LocalDateTime.now())
                   .build();

           Folder savedFolder =  folderMogoRepository.save(folder);

           return savedFolder.getId();

        }
        return null;

    }

    public String createRootDrive(NewDriveRequest request){

        String uid = request.getOwner();
        String makeDrivePath =  sftpService.createRootFolder(request.getName(),uid);

        log.info("결과!!!!"+makeDrivePath);

        if(makeDrivePath != null){
            Folder folder =  Folder.builder()
                    .name(request.getName())
                    .order(0.0)
                    .parentId(null)
                    .path(makeDrivePath)
                    .ownerId(uid)
                    .description(request.getDescription())
                    .status(0)
                    .isShared(request.getIsShared())
                    .linkSharing(request.getLinkSharing())
                    .updatedAt(LocalDateTime.now())
                    .build();

            Folder savedFolder =  folderMogoRepository.save(folder);

            return savedFolder.getId();

        }
        return null;

    }


    public List<FolderDto> getFoldersByUid(String uid,String parentId){
        List<Folder> folders = folderMogoRepository.findByOwnerIdAndAndParentIdOrderByOrder(uid,parentId);
            List<FolderDto> folderDtos = folders.stream().map(folder -> {
                FolderDto folderDto = FolderDto.builder()
                        .id(folder.getId())
                        .name(folder.getName())
                        .order(folder.getOrder())
                        .createdAt(folder.getCreatedAt())
                        .isShared(folder.getIsShared())
                        .isPinned(folder.getIsPinned())
                        .build();
                return folderDto;
            }).collect(Collectors.toList());
            return folderDtos;


    }


    public Folder getFolderName(String uid){
        return folderMogoRepository.findByName(uid);
    }


    public List<FolderDto> getSubFolders(String ownerId, String folderId){
        List<Folder> folders =folderMogoRepository.findByOwnerIdAndAndParentIdOrderByOrder(ownerId,folderId);


        return folders.stream().map(Folder::toDTO).collect(Collectors.toList());
    }

    public List<FileRequestDto> getFiles(String folderId){
        List<FileMogo> files =fileMogoRepository.findByFolderId(folderId);
        return files.stream()
                .map(FileRequestDto::toDto)
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
                    .owner_uid(uid)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .size(file.getSize())
                    .build();

            FileMogo saved = filedto.toEntity();

            // 임시 파일 생성
            File tempFile = null;
            try {
                tempFile = File.createTempFile("upload_", "_" + originalFilename);
                file.transferTo(tempFile); // MultipartFile 데이터를 임시 파일로 저장

                // SFTP 업로드
                sftpService.uploadFile(tempFile.getAbsolutePath(), remoteDir, savedFilename);

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


}
