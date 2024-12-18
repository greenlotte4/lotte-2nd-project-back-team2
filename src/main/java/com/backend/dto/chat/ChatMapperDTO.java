package com.backend.dto.chat;

import com.backend.document.chat.ChatMapperDocument;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ChatMapperDTO {

    private String id;

    private String userId;
    private String chatRoomId;
    private LocalDateTime lastReadTimeStamp; // 사용자가 마지막으로 읽은 메시지 시간
    private LocalDateTime joinedAt; // 사용자가 채팅방에 가입한 시간
    private int isFrequent; // 즐겨찾기 여부

    public ChatMapperDocument toDocument() {
        return ChatMapperDocument.builder()
                .id(this.id)
                .userId(this.userId)
                .chatRoomId(this.chatRoomId)
                .lastReadTimeStamp(this.lastReadTimeStamp)
                .joinedAt(this.joinedAt)
                .isFrequent(this.isFrequent)
                .build();
    }
}
