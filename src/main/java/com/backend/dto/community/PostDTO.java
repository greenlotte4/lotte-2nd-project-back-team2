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

    private boolean favoritePost; //즐겨찾기

    private boolean isMandatory; // 필독 여부

    private Long boardId; // 게시글이 속한 게시판

    private String uid; // 작성자 정보

    private String boardName;

    private LocalDateTime createdAt; // 생성 날짜

    public void setBoardName(String boardName) {
    }
}
