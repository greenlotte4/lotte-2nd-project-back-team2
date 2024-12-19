package com.backend.controller;

import com.backend.document.drive.Folder;
import com.backend.dto.chat.UsersWithGroupNameDTO;
import com.backend.dto.request.LoginDto;
import com.backend.dto.request.admin.user.PatchAdminUserApprovalDto;
import com.backend.dto.request.drive.NewDriveRequest;
import com.backend.dto.request.user.PaymentInfoDTO;
import com.backend.dto.request.user.PostUserRegisterDTO;
import com.backend.dto.response.GetAdminUsersRespDto;
import com.backend.dto.response.UserDto;
import com.backend.dto.response.drive.FolderDto;
import com.backend.dto.response.user.GetUsersAllDto;
import com.backend.entity.group.Group;
import com.backend.entity.user.CardInfo;
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

    // 12.01 이상훈 전체유저 무한스크롤 요청  // 12.18 하진희 company 조건 추가
    @GetMapping("/users/all")
    public ResponseEntity<?> getAllUser2(
            @RequestParam int page,
            @RequestParam (value = "keyword", defaultValue = "") String keyword,
            @RequestParam(value = "id", defaultValue = "0") Long id,
            HttpServletRequest request
    ){
        Map<String, Object> map = new HashMap<>();
        Page<GetUsersAllDto> dtos;
        String company =(String) request.getAttribute("company");

        if(!keyword.equals("")&&id==0){
            dtos = userService.getUsersAllByKeyword(page,keyword);
        } else if (!keyword.equals("")&&id!=0) {
            dtos = userService.getUsersAllByKeywordAndGroup(page,keyword,id,company);
        } else if (keyword.equals("")&& id!=0) {
            dtos = userService.getUsersAllByGroup(page,id,company);
        } else {
            dtos = userService.getUsersAll(page,company);
        }

        if (dtos == null) {
            dtos = Page.empty(); // Page.empty()는 안전한 빈 Page 객체를 반환
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
        log.info("프로필 업로드 컨트롤러 "+file.getOriginalFilename());
        Long userId = Long.valueOf(auth.getName());
        try {
            Boolean result = userService.uploadProfile(userId, file);
            return ResponseEntity.ok().body(result);
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/my/message")
    public ResponseEntity<?> updateMessage(Authentication auth,
                                           @RequestBody String message ){
        log.info("프로필 메세지 컨트롤러 "+ message);
        Long userId = Long.valueOf(auth.getName());
        return userService.updateMessage(userId,message);
    }

    @PostMapping("/my/modify")
    public ResponseEntity<?> modifyUser(Authentication auth,
                                        @RequestBody PostUserRegisterDTO dto){
        log.info("회원 수정 컨트롤러 "+dto);
        Long userId = Long.valueOf(auth.getName());
        return userService.updateUser(userId, dto);
    }

    @PostMapping("/my/confirmPass")
    public ResponseEntity<?> confirmPass(Authentication auth,
                                         @RequestBody LoginDto pwd){
        log.info("비밀번호 확인 컨트롤러 "+pwd.getPwd());
        Long userId = Long.valueOf(auth.getName());
        return userService.confirmPass(userId, pwd.getPwd());
    }
    @PostMapping("/my/updatePass")
    public ResponseEntity<?> updatePass (Authentication auth,
                                         @RequestBody LoginDto dto){
        log.info("비밀번호 변경 컨트롤러 "+dto);
        Long userId = Long.valueOf(auth.getName());
        return userService.updatePass(userId, dto.getPwd());
    }

    @GetMapping("/my/cardInfos")
    public ResponseEntity<?> getCardInfo(Authentication auth){
        Long userId = Long.valueOf(auth.getName());
        ResponseEntity<?> result  = userService.getCardInfo(userId);
        return result;
    }

    @PostMapping("/my/deletePayment")
    public ResponseEntity<?> deletePayment(@RequestBody PaymentInfoDTO dto){
        log.info("카드 삭제 컨트롤러 "+dto.getPaymentId());
        Long paymentId = dto.getPaymentId();
        ResponseEntity<?> result = userService.deletePayment(paymentId);
        return result;
    }

    @PostMapping("/my/addPayment")
    public ResponseEntity<?> insertPayment (@RequestBody PaymentInfoDTO dto, Authentication auth){
        Long userId = Long.valueOf(auth.getName());
        ResponseEntity result  = userService.addPayment(dto, userId);
        return result;
    }

    @GetMapping("/my/deleteAccount")
    public ResponseEntity<?> deleteAccount (Authentication auth){
        Long userId = Long.valueOf(auth.getName());
        ResponseEntity<?> result = userService.deleteAccount(userId);
        return result;
    }
}
