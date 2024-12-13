package com.backend.dto.community;
/*
    날짜 : 2024/12/03
    이름 : 박서홍
    내용 : Board Entity 작성
 */

import com.backend.entity.community.BaseTimeEntity;
import com.backend.entity.community.Board;
import com.backend.entity.group.Group;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoardDTO {

    private Long boardId;

    private int status;

    private Long groupId;

    private String boardName;

    private String description;

    private boolean favoriteBoard;

    private LocalDateTime createdAt; // 생성 날짜

    private LocalDateTime updatedAt; // 수정 날짜


    // 에러에 명시된 형태의 생성자 추가
    public BoardDTO(Long boardId, String boardName, String description, boolean favoriteBoard) {
        this.boardId = boardId;
        this.boardName = boardName;
        this.description = description;
        this.favoriteBoard = favoriteBoard;
    }

}

