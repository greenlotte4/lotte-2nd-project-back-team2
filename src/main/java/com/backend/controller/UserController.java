package com.backend.controller;

import com.backend.document.drive.Folder;
import com.backend.dto.chat.UsersWithGroupNameDTO;
import com.backend.dto.request.admin.user.PatchAdminUserApprovalDto;
import com.backend.dto.request.drive.NewDriveRequest;
import com.backend.dto.response.GetAdminUsersRespDto;
import com.backend.dto.response.UserDto;
import com.backend.dto.response.drive.FolderDto;
import com.backend.dto.response.user.GetUsersAllDto;
import com.backend.entity.group.Group;
import com.backend.repository.UserRepository;
import com.backend.service.GroupService;
import com.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Log4j2
public class UserController {

    private final UserService userService;
    private final GroupService groupService;
    private final UserRepository userRepository;

    @GetMapping("/users")
    public ResponseEntity<?> getUser(){
        List<GetAdminUsersRespDto> users = userService.getUserNotTeamLeader();
        return ResponseEntity.ok(users);
    }

    // 11.29 전규찬 전체 사용자 조회
    @GetMapping("/allUsers")
    public ResponseEntity<?> getAllUserWithGroupName(){
        List<UsersWithGroupNameDTO> users = userService.getAllUsersWithGroupName();
        return ResponseEntity.ok(users);
    }

    // 12.01 이상훈 전체유저 무한스크롤 요청
    @GetMapping("/users/all")
    public ResponseEntity<?> getAllUser2(
            @RequestParam int page,
            @RequestParam (value = "keyword", defaultValue = "") String keyword,
            @RequestParam(value = "id", defaultValue = "0") Long id
    ){
        Map<String, Object> map = new HashMap<>();
        Page<GetUsersAllDto> dtos;
        if(!keyword.equals("")&&id==0){
            dtos = userService.getUsersAllByKeyword(page,keyword);
        } else if (!keyword.equals("")&&id!=0) {
            dtos = userService.getUsersAllByKeywordAndGroup(page,keyword,id);
        } else if (keyword.equals("")&& id!=0) {
            dtos = userService.getUsersAllByGroup(page,id);
        } else {
            dtos = userService.getUsersAll(page);
        }

        map.put("users", dtos.getContent());
        map.put("totalPages", dtos.getTotalPages());
        map.put("totalElements", dtos.getTotalElements());
        map.put("currentPage", dtos.getNumber());
        map.put("hasNextPage", dtos.hasNext());

        return ResponseEntity.ok().body(map);
    }

    @GetMapping("/users/all/search")
    public ResponseEntity<?> getAllUsersBySearch(
            @RequestParam String search
    ){
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/admin/user/approval")
    public ResponseEntity<?> patchUserApproval(
            @RequestBody PatchAdminUserApprovalDto dto
            ){
        ResponseEntity<?> response = userService.patchUserApproval(dto);
        return response;
    }

    @GetMapping("/users/all/cnt")
    public ResponseEntity<?> getAllUserCnt(){
        String company = "1246857";
        ResponseEntity<?> response = userService.getALlUsersCnt(company);
        return response;
    }


    @GetMapping("/user/id")
    public ResponseEntity<?> getCalendarGroups (
            HttpServletRequest req
    ){
        Object idObj = req.getAttribute("id");
        Long id;
        if (idObj != null) {
            id = Long.valueOf(idObj.toString());  // 문자열을 Long으로 변환
        } else {
            id= 0L;
        }

        return ResponseEntity.ok(id);
    }


    @GetMapping("/my/user")
    public ResponseEntity<?> getMyUser (Authentication auth){
        Long userId = Long.valueOf(auth.getName());
        try {
            UserDto user = userService.getMyUser(userId);
            log.info("유저 정보 "+user.toString());
            return ResponseEntity.ok(user);
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/my/profile")
    public ResponseEntity<?> uploadProfile(Authentication auth,
                                           @RequestParam("file") MultipartFile file
    ){
        log.info("프로필 업로드 컨트롤러 "+file);
        log.info("프로필 업로드 컨트롤러 "+file.getOriginalFilename());
        log.info("프로필 업로드 컨트롤러 "+file.getName());
        Long userId = Long.valueOf(auth.getName());
        try {
            Boolean result = userService.uploadProfile(userId, file);
            return ResponseEntity.ok().body(result);
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
