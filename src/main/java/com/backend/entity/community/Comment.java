package com.backend.entity.community;

/*
    날짜 : 2024/12/03
    이름 : 박서홍
    내용 : Comment Entity 작성
 */

import com.backend.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Entity
public class Comment extends BaseTimeEntity{
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "comment_id")
    private Long commentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id") // 부모 댓글 ID
    private Comment parent; // 부모 댓글 (대댓글 기능용)

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> children = new ArrayList<>(); // 자식 댓글 (대댓글 리스트)

    private String content;

    private String writer;

    @ManyToOne(fetch = FetchType.LAZY) // 작성자는 하나의 사용자
    @JoinColumn(name = "user_id", nullable = false) // 외래 키
    private User user; // 작성자 정보

}
