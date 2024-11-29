package com.backend.controller;

import com.backend.dto.request.LoginDto;
import com.backend.dto.request.admin.user.PatchAdminUserApprovalDto;
import com.backend.dto.response.GetAdminUsersRespDto;
import com.backend.dto.response.UserDto;
import com.backend.entity.user.User;
import com.backend.repository.UserRepository;
import com.backend.service.UserService;
import com.backend.util.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:8010")
@RequiredArgsConstructor
@Log4j2
public class UserController {

    private final UserService userService;

    @GetMapping("/users")
    public ResponseEntity<?> getUser(){
        List<GetAdminUsersRespDto> users = userService.getUserNotTeamLeader();
        System.out.println(users);
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/admin/user/approval")
    public ResponseEntity<?> patchUserApproval(
            @RequestBody PatchAdminUserApprovalDto dto
            ){
        ResponseEntity<?> response = userService.patchUserApproval(dto);
        return response;
    }
}
