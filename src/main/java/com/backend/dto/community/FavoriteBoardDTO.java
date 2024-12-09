package com.backend.dto.community;


import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Builder
public class FavoriteBoardDTO {
    private Long itemId;
    private String itemType;
    private String itemName; // 예: 게시판 이름 또는 게시글 제목


    // 매개변수를 받는 생성자 추가
    public FavoriteBoardDTO(Long itemId, String itemType, String itemName) {
        this.itemId = itemId;
        this.itemType = itemType;
        this.itemName = itemName;
    }


}
