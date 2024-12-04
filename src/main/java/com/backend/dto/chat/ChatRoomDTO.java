package com.backend.dto.chat;

import com.backend.document.chat.ChatRoomDocument;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatRoomDTO {

    private String id;

    @Builder.Default
    private int status = 1; // 상태
    @Builder.Default
    private int chatRoomFavorite = 0; // 즐겨찾기
    @Builder.Default
    private int chatRoomReadCnt = 0; // 안읽은메세지수
    private String chatRoomName; // 채팅방이름
    private String leader;  // 방장 uid
    private List<String> members;  // 채팅방 구성원 uid
    private LocalDateTime lastUpdated; // 마지막으로 업데이트된 시간


    public ChatRoomDocument toDocument() {
        return ChatRoomDocument.builder()
                .id(this.getId())
                .status(this.getStatus())
                .chatRoomFavorite(this.getChatRoomFavorite())
                .chatRoomReadCnt(this.getChatRoomReadCnt())
                .chatRoomName(this.getChatRoomName())
                .leader(this.getLeader())
                .members(this.getMembers())
                .lastUpdated(this.getLastUpdated())
                .build();
    }

}
