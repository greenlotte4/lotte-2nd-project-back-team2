package com.backend.dto.community;

import com.backend.entity.community.Comment;
import com.backend.entity.community.Post;
import com.backend.entity.user.User;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponseDTO {

    private Long commentId;

    private Long postId;

    private String content;

    private String writer;

    private LocalDateTime createdAt;

    private Long parentId;

    private String mentionUsername;

    private List<CommentResponseDTO> children;

    private Integer depth;

    private Long likesCount;

    private Boolean isDeleted;

    private boolean isLiked;


    // Comment 객체를 기반으로 하는 생성자
    public CommentResponseDTO(Comment comment, boolean isLiked) {
        this.commentId = comment.getCommentId();
        this.postId = comment.getPost().getPostId();
        this.content = comment.getContent();
        this.writer = comment.getUser().getName();
        this.createdAt = comment.getCreatedAt();
        this.likesCount = comment.getLikes();
        this.isDeleted = comment.getIsDeleted();
        this.depth = comment.getDepth();
        this.isLiked = isLiked;

    }

}
