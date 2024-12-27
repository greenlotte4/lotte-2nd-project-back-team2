package com.backend.controller;

import com.backend.document.BoardFile;
import com.backend.dto.community.PostDTO;
import com.backend.entity.community.Board;
import com.backend.repository.community.BoardRepository;
import com.backend.service.PostService;
import com.backend.service.SftpService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@RestController
@Log4j2
@RequestMapping("/api/community")
@CrossOrigin(origins = "http://localhost:8010") // 프론트엔드의 도메인

@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final BoardRepository boardRepository;
    private final SftpService sftpService;

    @GetMapping("/write")
    public ResponseEntity<?> getUid(Authentication authentication) {
        log.info("뭔데 ");
        String uid = authentication.getName();
        log.info("잘들어오냐" + uid);
        return postService.getUid(uid);
    }

    @PostMapping("/write")
    public ResponseEntity<?> createPost(@ModelAttribute PostDTO postDTO,
                                        @RequestParam(value = "files", required = false) List<MultipartFile> files,
                                        HttpServletRequest request) {
        log.info("글쓰기 컨트롤러 호출");

        String uid = (String) request.getAttribute("uid");
        postDTO.setUid(uid);  // UID 설정
        log.info("Received PostDTO: " + postDTO);

        try {
            // 파일 처리 로직
            if (files != null && !files.isEmpty()) {
                // 파일 처리 예시: 파일 저장
            }

            // 게시글 생성
            ResponseEntity<?> resultPostDTO = postService.createPost(postDTO);

            return ResponseEntity.status(HttpStatus.CREATED).body(resultPostDTO);

        } catch (Exception e) {
            log.error("게시글 생성 중 오류 발생: " + e.getMessage(), e);

            // 실패 시 에러 메시지를 ResponseEntity에 포함하여 반환
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("게시글 생성 실패: " + e.getMessage());
        }
    }



    @GetMapping("/posts")
    public ResponseEntity<List<PostDTO>> getView(@RequestParam Long boardId, Authentication authentication, Pageable pageable) {
        // 게시판 존재 여부 확인
        Optional<Board> boardOpt = boardRepository.findById(boardId);
        if (boardOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(null);  // 존재하지 않는 게시판
        }
        Board board = boardOpt.get();

        // 부서별 게시판에 대한 권한 체크 부분 제거
        // if (board.getBoardType() == 2) {
        //     String uid = authentication.getName();
        //     boolean hasAccess = postService.checkBoardAccess(boardId, uid);
        //     if (!hasAccess) {
        //         return ResponseEntity.status(403).body(null);  // 권한 없음
        //     }
        // }

        List<PostDTO> posts = postService.getPostsByBoardId(boardId, pageable);
        return ResponseEntity.ok(posts);  // 정상적으로 게시글 반환
    }


    @GetMapping("/view")
    public ResponseEntity<?> viewPost(@RequestParam Long postId,
                                      @RequestParam Long boardId,
                                      Authentication authentication) {
        try {
            log.info("게시글 상세보기: postId = {} , boardId = {}", postId, boardId);

            String uid = authentication.getName(); // 인증된 사용자 ID
            log.info("인증된 사용자 ID: {}", uid);

            // 게시물 조회
            PostDTO postDTO = postService.getPostById(boardId, postId);
            if (postDTO == null) {
                log.error("게시물 조회 실패: 게시물이 존재하지 않습니다.");
                return ResponseEntity.status(404).body("게시물이 존재하지 않습니다.");
            }



            // 첨부파일 목록 조회
            List<BoardFile> files = postService.getFilesByPostId(postId);
            postDTO.setSavedFiles(files);

            // 게시물 상세 정보 반환
            return ResponseEntity.ok(postDTO);

        } catch (Exception e) {
            log.error("서버 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("서버 오류 발생");
        }
    }

    @GetMapping("/view/download")
    public ResponseEntity<String> downloadFile(@RequestParam("filePath") String filePath) {
        log.info("파일 다운로드 요청: {}", filePath);

        // 파일 경로가 null이 아니고, 유효한 경로인지 확인
        if (filePath == null || filePath.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("파일 경로가 유효하지 않습니다.");
        }

        // SFTP로 파일 다운로드 처리
        String remoteDir = filePath.substring(0, filePath.lastIndexOf("/")); // 디렉토리 경로
        String remoteFileName = filePath.substring(filePath.lastIndexOf("/") + 1); // 파일 이름

        String localFilePath = "/local/downloads/" + remoteFileName;

        boolean downloadSuccess = postService.downloadFile(remoteDir, remoteFileName, localFilePath);

        if (downloadSuccess) {
            return ResponseEntity.ok("파일 다운로드 성공");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("파일 다운로드 실패");
        }
    }





    @GetMapping("/posts/{boardId}/{postId}")
    public ResponseEntity<PostDTO> getPostById(@PathVariable Long boardId, @PathVariable Long postId) {
        PostDTO postDTO = postService.getPostById(boardId, postId);
        return ResponseEntity.ok(postDTO);
    }
    @PutMapping("/posts/{boardId}/view/{postId}")
    public ResponseEntity<String> updatePost(
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @ModelAttribute PostDTO postDTO) {

        log.info("게시글 수정 요청 - boardId: {}, postId: {}, title: {}", boardId, postId, postDTO.getTitle());

        try {
            // 파일 처리 로직 추가
            postService.updatePost(boardId, postId, postDTO, files);  // 서비스로 파일과 DTO를 전달

            return ResponseEntity.ok("게시글이 성공적으로 수정되었습니다.");
        } catch (Exception e) {
            log.error("게시글 수정 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("게시글 수정 실패");
        }
    }





    @DeleteMapping("/posts/{boardId}/view/{postId}")
    public ResponseEntity<String> deletePost(@PathVariable Long boardId, @PathVariable Long postId) {
        try {
            postService.deletePost(boardId, postId);
            return ResponseEntity.ok("게시글이 삭제되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("게시글 삭제 실패: " + e.getMessage());
        }
    }
    @GetMapping("/posts/search")
    public ResponseEntity<Page<PostDTO>> getPosts(
            @RequestParam Long boardId,
            @RequestParam(required = false) String keyword,
            Pageable pageable
    ) {
        log.info("searchPosts endpoint called with boardId: {}, keyword: '{}'", boardId, keyword);

        String searchKeyword = (keyword != null) ? keyword : "";

        Page<PostDTO> filteredPosts = postService.getFilteredPosts(boardId, searchKeyword, pageable);

        return ResponseEntity.ok(filteredPosts);
    }


}

