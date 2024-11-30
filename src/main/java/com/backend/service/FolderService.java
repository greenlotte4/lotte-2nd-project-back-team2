package com.backend.service;


import com.backend.dto.request.drive.MoveFolderRequest;
import com.backend.dto.request.drive.NewDriveRequest;
import com.backend.dto.response.drive.FolderDto;
import com.backend.entity.folder.Folder;
import com.backend.entity.folder.Permission;
import com.backend.entity.user.User;
import com.backend.repository.FolderMogoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class FolderService {

    private static final String BASE_UPLOAD_DIR = "/data/uploads/";
    private final SftpService sftpService;
    private final UserService userService;
    private final FolderMogoRepository folderMogoRepository;
    private String fileServerUrl = "http://43.202.45.49:90/local/upload/create-folder";


    @Value("${spring.servlet.multipart.location}")
    private String uploadPath;

    public void makeDir(String folderName){

        RestTemplate restTemplate = new RestTemplate();
        String apiUrl = fileServerUrl;

        // 요청 헤더와 바디 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        String requestBody = "{\"folderName\":\"" + folderName + "\"}";

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        // API 호출
        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            File folder = new File(BASE_UPLOAD_DIR+folderName);

            if (!folder.exists()) {
                if (folder.mkdirs()) {
                    System.out.println("Folder created successfully"+ folderName);

                } else {
                    System.out.println("Failed to create folder"+ folderName);


                }
            } else {
                System.out.println("Folder already exists"+ folderName);

            }
            System.out.println("폴더 생성 성공: " + folderName);
        } else {
            System.err.println("폴더 생성 실패: " + response.getStatusCode());
        }
    }


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
            folder.newFileName(newName);
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


}
