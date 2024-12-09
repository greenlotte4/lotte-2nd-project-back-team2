package com.backend.dto.community;


import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardListResponseDTO {

    private Long boardId;
    private String boardName;
}
