package com.backend.dto.community;

/*
    날짜 : 2024/12/03
    이름 : 박서홍
    내용 : Post Entity 작성
 */

import com.backend.entity.community.BaseTimeEntity;
import com.backend.entity.community.Board;
import com.backend.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDTO {


    private Long postId;

    private String writer;

    private String title;

    private String content;

    private int fileCount;

    private String regip;

    private boolean favoritePost;

    private MultipartFile file;

    private boolean isMandatory;

    private Long boardId;

    private String uid;

    private String boardName;

    private LocalDateTime createdAt;

    public void setBoardName(String boardName) {
    }
}
