package com.backend.entity.community;
/*
    날짜 : 2024/12/03
    이름 : 박서홍
    내용 : Board Entity 작성
 */

import com.backend.dto.community.BoardDTO;
import com.backend.entity.group.Group;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Board extends BaseTimeEntity{
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "board_id")
    private Long boardId; // 게시판 ID

    private int status; // 1 공지사항 2 익명게시판 3 자유게시판 4 자료실 5 오늘의 식단 6 부서별 게시판

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;


    @Column(nullable = false, unique = true)
    private String boardName; // 게시판 이름 (예: 공지사항, 자유게시판 등)

    private String description; // 게시판 설명

    @Column(name = "favorite_Board", nullable = false)
    private boolean favoriteBoard; //즐겨찾기

    // Board 엔티티의 DTO 변환 메서드
    public BoardDTO toDTO() {
        return BoardDTO.builder()
                .boardId(this.boardId)
                .boardName(this.boardName)
                .description(this.description)
                .favoriteBoard(this.favoriteBoard)
                .groupId(this.group != null ? this.group.getId() : null)
                .status(this.status)
                .createdAt(this.getCreatedAt()) // BaseTimeEntity의 필드
                .updatedAt(this.getUpdatedAt()) // BaseTimeEntity의 필드
                .build();
    }

}
