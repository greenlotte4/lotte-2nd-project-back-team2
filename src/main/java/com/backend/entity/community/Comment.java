package com.backend.entity.community;

import com.backend.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long commentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent; // 부모 댓글 (대댓글 기능용)

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> children = new ArrayList<>(); // 자식 댓글 (대댓글 리스트)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private Long likes = 0L;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @ElementCollection(fetch = FetchType.EAGER) // 즉시 로딩으로 변경하여 프록시 문제 방지
    private Set<Long> likedUserIds = new HashSet<>(); // 좋아요 누른 회원 ID 목록

    @Column(nullable = false)
    private int depth = 0; // 댓글 깊이 (0: 일반 댓글, 1 이상: 대댓글)

    @Column(nullable = false)
    private Long orderNumber; // 댓글 순서

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mention_user_id")
    private User mentionUser;

    // 댓글 생성자
    public Comment(Post post, Comment parent, User user, String content) {
        this.post = post;
        this.parent = parent;
        this.user = user;
        this.content = content;
        this.likes = 0L;
        this.isDeleted = false;

        if (parent == null) {
            // 부모 댓글이 없는 경우 (일반 댓글)
            this.depth = 0;
            this.orderNumber = post.getCommentCount() + 1L;
        } else {
            // 부모 댓글이 있는 경우 (대댓글)
            this.depth = parent.getDepth() + 1;
            this.orderNumber = parent.getOrderNumber();
        }
    }

    // 댓글 삭제 (소프트 삭제)
    public void deleteComment() {
        this.isDeleted = true;
    }

    // 사용자별 좋아요 토글 메서드
    public boolean toggleLike(Long userId) {
        if (likedUserIds.contains(userId)) {
            likedUserIds.remove(userId);
            decreaseLikes();
            return false; // 좋아요 취소
        } else {
            likedUserIds.add(userId);
            increaseLikes();
            return true; // 좋아요 추가
        }
    }

    // 좋아요 증가
    public void increaseLikes() {
        this.likes++;
    }

    // 좋아요 감소
    public void decreaseLikes() {
        if (this.likes > 0) {
            this.likes--;
        }
    }
}
