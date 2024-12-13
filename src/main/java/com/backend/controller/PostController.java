package com.backend.controller;

import com.backend.dto.community.PostDTO;
import com.backend.entity.community.Board;
import com.backend.repository.community.BoardRepository;
import com.backend.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@Log4j2
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final BoardRepository boardRepository;

    @GetMapping("/write")
    public ResponseEntity<?> getUid(Authentication authentication) {
        log.info("뭔데 ");
        String uid = authentication.getName();
        log.info("잘들어오냐" + uid);
        return postService.getUid(uid);
    }

    @PostMapping("/write")
    public ResponseEntity<PostDTO> createPost(@RequestBody PostDTO postDTO, HttpServletRequest request) {
        log.info("글쓰기 컨트롤러 ");
        String uid = (String) request.getAttribute("uid");
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.length() == 0 || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("X-Real-IP");
        }
        if (clientIp == null || clientIp.length() == 0 || "unknown".equalsIgnoreCase(clientIp)) {
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

    @GetMapping("/posts")
    public ResponseEntity<List<PostDTO>> getView(@RequestParam Long boardId, Authentication authentication, Pageable pageable) {
        // 게시판 존재 여부 확인
        Optional<Board> boardOpt = boardRepository.findById(boardId);
        log.info("boardOpt: " + boardOpt);
        if (boardOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(null);  // 존재하지 않는 게시판
        }
        Board board = boardOpt.get();


        PostDTO postDTO = new PostDTO();
        postDTO.setBoardName(board.getBoardName());
        String boardName = postDTO.getBoardName();

        if (board.getBoardType() == 2) { // 부서별 게시판
            String uid = authentication.getName();
            boolean hasAccess = postService.checkBoardAccess(boardId, uid);  // boardName 추가

            if (!hasAccess) {
                return ResponseEntity.status(403).body(null);  // 권한 없음
            }
        }
        List<PostDTO> posts = postService.getPostsByBoardId(boardId, pageable);
        return ResponseEntity.ok(posts);  // 정상적으로 게시글 반환
    }

    @GetMapping("/view")
    public ResponseEntity<PostDTO> viewPost(@RequestParam Long postId, @RequestParam Long boardId, Authentication authentication) {
        try {
            log.info("게시글 상세보기: postId = " + postId + ", boardId = " + boardId);

            String uid = authentication.getName();  // 인증된 사용자 ID
            log.info("인증된 사용자 ID: " + uid);

            // 게시물 조회
            PostDTO postDTO = postService.getPostById(boardId, postId);
            log.info("게시물 조회 결과: " + (postDTO != null ? postDTO : "게시물 없음"));

            if (postDTO == null) {
                log.error("게시물 조회 실패: 게시물이 존재하지 않습니다.");

                return ResponseEntity.status(404).body(null);  // 게시물이 없으면 404 반환
            }

            Long postBoardId = postDTO.getBoardId(); // boardId

            // 사용자가 게시물에 접근할 수 있는지 확인 (권한 체크)
            boolean hasAccess = postService.checkBoardAccess(postBoardId, uid);
            log.info("게시판 접근 권한 확인 결과: " + hasAccess);
            if (!hasAccess) {
                log.warn("사용자 권한 없음: 접근 불가");

                return ResponseEntity.status(403).body(null);  // 권한 없음
            }

            return ResponseEntity.ok(postDTO);  // 게시물 상세 정보 반환


        } catch (Exception e) {
            log.error("서버 오류 발생: " + e.getMessage(), e);  // 예외 로그 출력
            return ResponseEntity.status(500).body(null);
        }

    }
}


