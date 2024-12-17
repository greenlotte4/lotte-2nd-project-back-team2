package com.backend.controller;

import com.backend.dto.community.CommentRequestDTO;
import com.backend.dto.community.CommentResponseDTO;
import com.backend.entity.community.Comment;
import com.backend.entity.community.Post;
import com.backend.entity.user.User;
import com.backend.repository.UserRepository;
import com.backend.repository.community.CommentRepository;
import com.backend.repository.community.PostRepository;
import com.backend.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Log4j2
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/community/posts")
public class CommentController {

    private final CommentService commentService;
    private final CommentRepository commentRepository;


    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<CommentResponseDTO>> getComments(@PathVariable Long postId) {
        List<CommentResponseDTO> comments = commentService.getCommentsByPostId(postId);
        return ResponseEntity.ok(comments);
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<String> createComment(@RequestBody CommentRequestDTO requestDto) {

        commentService.createComment(requestDto);
        return ResponseEntity.ok("댓글이 성공적으로 등록되었습니다.");
    }


    @PutMapping("/{postId}/{commentId}")
    public ResponseEntity<String> updateComment(@PathVariable Long commentId, @RequestBody CommentRequestDTO requestDto) {
        commentService.updateComment(commentId, requestDto);
        return ResponseEntity.ok("댓글이 성공적으로 수정되었습니다.");
    }

    @DeleteMapping("/{postId}/{commentId}")
    public ResponseEntity<String> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok("댓글이 성공적으로 삭제되었습니다.");
    }

    @PostMapping("/{postId}/comments/{commentId}/like")
    public ResponseEntity<CommentResponseDTO> likeComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestParam Long userId) {

        log.info("=== 좋아요 API 호출 시작 ===");
        log.info("RequestMapping: /api/community/posts");
        log.info("PostMapping: /{postId}/comments/{commentId}/like");
        log.info("전체 URL: /api/community/posts/{}/comments/{}/like", postId, commentId);
        log.info("받은 파라미터 - postId: {}, commentId: {}", postId, commentId);

        try {
            CommentResponseDTO responseDto = commentService.likeComment(postId, commentId, userId);
            log.info("좋아요 처리 완료 - 댓글 ID: {}, 좋아요 수: {}",
                    responseDto.getCommentId(), responseDto.getLikesCount());
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            log.error("좋아요 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(null);
        }
    }
}



