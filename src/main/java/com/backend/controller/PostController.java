package com.backend.controller;

import com.backend.dto.community.PostDTO;
import com.backend.entity.community.Board;
import com.backend.entity.community.Post;
import com.backend.entity.user.User;
import com.backend.repository.UserRepository;
import com.backend.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@Log4j2
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

//    public PostController(PostService postService) {
//        this.postService = postService;
//    }

    @GetMapping("/write")
    public ResponseEntity<?> getUid(Authentication authentication) {
        log.info("뭔데 ");
        String uid = authentication.getName();
        log.info("잘들어오냐" + uid);
        return postService.getUid(uid);
//        return null;
    }

    @PostMapping("/write")
    public ResponseEntity<PostDTO> createPost(@RequestBody PostDTO postDTO, Authentication authentication, HttpServletRequest request) {
        log.info("글쓰기 컨트롤러 ");
        String uid = (String) request.getAttribute("uid");
        String clientIp = request.getHeader("X-Forwarded-For");
        if(clientIp == null || clientIp.length() == 0 || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("X-Real-IP");
        }
        if(clientIp == null || clientIp.length() == 0 || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getRemoteAddr();
        }
        postDTO.setUid(uid);
        postDTO.setRegip(clientIp);
        log.info("postDTO: " + postDTO);
        // 게시글 생성
        ResponseEntity result = postService.createPost(postDTO);

        // 응답 반환
        return result;
    }

}
