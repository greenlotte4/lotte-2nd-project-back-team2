package com.backend.controller;

import com.backend.dto.chat.UsersWithGroupNameDTO;
import com.backend.dto.request.admin.user.PatchAdminUserApprovalDto;
import com.backend.dto.response.GetAdminUsersRespDto;
import com.backend.dto.response.user.GetUsersAllDto;
import com.backend.entity.group.Group;
import com.backend.service.AttendanceService;
import com.backend.service.GroupService;
import com.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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


}
