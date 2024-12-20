package com.backend.entity.community;

/*
    날짜 : 2024/12/03
    이름 : 박서홍
    내용 : Post Entity 작성
 */

import com.backend.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
@Entity

public class Post extends BaseTimeEntity{
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;

    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "comment_count")
    @Builder.Default
    private int commentCount = 0;

    @Column(name = "file_count")
    @Builder.Default
    private int fileCount = 0;
    @Builder.Default
    private int hit=0;

    private String writer;

    private String regip;

    @Column(nullable = false)
    @Builder.Default
    private boolean favoritePost = false; //즐겨찾기

    private boolean isMandatory; // 필독 여부

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board; // 게시글이 속한 게시판

    @ManyToOne(fetch = FetchType.LAZY) // 작성자는 하나의 사용자
    @JoinColumn(name = "user_id", nullable = false) // 외래 키
    private User user; // 작성자 정보

    @OneToMany(mappedBy = "post", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @Column(name = "file_name")
    private String fileName; // 파일 이름

    @Column(name = "file_path")
    private String filePath; // 파일 저장 경로

}
