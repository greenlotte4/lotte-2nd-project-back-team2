package com.backend.controller;

import com.backend.dto.request.user.PostUserAlarmDto;
import com.backend.repository.UserRepository;
import com.backend.dto.response.user.RespHeaderUserDTO;
import com.backend.entity.user.User;
import com.backend.repository.UserRepository;
import com.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class MainController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping(value = {"/","/index"})
    public ResponseEntity<?> index() {

        return ResponseEntity.ok("SU");
    }

    @PostMapping("/api/alert")
    public ResponseEntity<?> postAlert(
            @RequestBody PostUserAlarmDto dto,
            HttpServletRequest req
    ){
        Object idObj = req.getAttribute("id");
        Long id;
        if (idObj != null) {
            id = Long.valueOf(idObj.toString());  // 문자열을 Long으로 변환
        } else {
            id= 0L;
        }
        ResponseEntity<?> response = userService.postAlert(dto,id);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/user/name")
    public ResponseEntity<?> getUserName(HttpServletRequest req) {
        Object idObj = req.getAttribute("id");
        Long id;
        if (idObj != null) {
            id = Long.valueOf(idObj.toString());  // 문자열을 Long으로 변환
        } else {
            id= 0L;
        }
        User user = userRepository.findById(id).get();

        RespHeaderUserDTO header = RespHeaderUserDTO.builder()
                .name(user.getName())
                .profileImgPath(user.getProfileImgPath() != null ? user.getProfileImgPath() : "Default.png")
                .build();
        return ResponseEntity.ok(header);
    }

}
