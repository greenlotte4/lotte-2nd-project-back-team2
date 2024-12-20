package com.backend.repository.community;

import com.backend.entity.community.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPost_PostId(Long postId);
    List<Comment> findByPost_PostIdAndPost_Board_BoardId(Long postId, Long boardId);
    List<Comment> findByPost_PostIdAndParentIsNull(Long postId);
    Optional<Comment> findByPost_PostIdAndCommentId(Long postId, Long commentId);
    @Transactional
    void deleteByPost_PostId(Long postId);
}
