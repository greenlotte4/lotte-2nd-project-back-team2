package com.backend.service;

import com.backend.dto.community.CommentRequestDTO;
import com.backend.dto.community.CommentResponseDTO;
import com.backend.entity.community.Comment;
import com.backend.entity.community.Post;
import com.backend.entity.user.User;
import com.backend.repository.UserRepository;
import com.backend.repository.community.CommentRepository;
import com.backend.repository.community.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@RequiredArgsConstructor
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;




    public CommentResponseDTO convertToResponseDto(Comment comment) {
        CommentResponseDTO dto = new CommentResponseDTO();
        dto.setCommentId(comment.getCommentId());
        dto.setPostId(comment.getPost().getPostId());
        dto.setContent(comment.getContent());
        dto.setWriter(comment.getUser().getName());
        dto.setUid(comment.getUser().getUid()); // **User 엔티티에서 uid 가져오기**

        dto.setCreatedAt(comment.getCreatedAt());
        dto.setParentId(comment.getParent() != null ? comment.getParent().getCommentId() : null);
        dto.setMentionUsername(comment.getMentionUser() != null ? comment.getMentionUser().getName() : null);
        dto.setLikesCount(comment.getLikes());
        dto.setIsDeleted(comment.getIsDeleted());
        dto.setDepth(comment.getDepth());

        // 대댓글 리스트를 재귀적으로 DTO로 변환
        if (comment.getChildren() != null && !comment.getChildren().isEmpty()) {
            List<CommentResponseDTO> childDtos = comment.getChildren()
                    .stream()
                    .map(this::convertToResponseDto)
                    .collect(Collectors.toList());
            dto.setChildren(childDtos);
        }
        return dto;
    }

    public List<CommentResponseDTO> getCommentsByPostId(Long postId) {
        log.info("댓글 조회 요청: postId = {}", postId);

        List<Comment> comments = commentRepository.findByPost_PostIdAndParentIsNull(postId);
        log.info("조회된 최상위 댓글 수: {}", comments.size());

        return comments.stream()
                .map(comment -> {
                    log.info("댓글 내용: {}, 작성자: {}", comment.getContent(),
                            comment.getUser() != null ? comment.getUser().getName() : "작성자 없음");
                    return convertToResponseDto(comment);
                })
                .collect(Collectors.toList());
    }


    @Transactional
    public Comment createComment(CommentRequestDTO requestDto) {
        // 게시글 조회
        Post post = postRepository.findById(requestDto.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        // 부모 댓글 조회 (대댓글일 경우)
        Comment parentComment = null;
        int depth = 0; // 기본 depth는 0 (최상위 댓글)
        if (requestDto.getParentId() != null) {
            parentComment = commentRepository.findById(requestDto.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("부모 댓글이 존재하지 않습니다."));
            depth = parentComment.getDepth() + 1; // 대댓글인 경우 깊이를 1 증가
        }

        // 사용자 조회
        User user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));

        // 댓글 생성
        Comment comment = new Comment(post, parentComment, user, requestDto.getContent());
        comment.setDepth(depth); // 깊이 설정

        // 댓글 저장
        return commentRepository.save(comment);
    }


    @Transactional
    public void updateComment(Long commentId, CommentRequestDTO requestDto) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));

        // 댓글 내용 수정
        comment.setContent(requestDto.getContent());
    }

    // 댓글 삭제 (소프트 삭제)
    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));

        // 논리 삭제 처리
        comment.setIsDeleted(true);
    }

    @Transactional
    public CommentResponseDTO likeComment(Long postId, Long commentId, Long userId) {
        Comment comment = commentRepository.findByPost_PostIdAndCommentId(postId, commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));

        boolean isLiked = comment.toggleLike(userId);

        commentRepository.save(comment);

        return new CommentResponseDTO(comment, isLiked);
    }

}

