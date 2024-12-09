package com.backend.document.chat;

import com.backend.dto.chat.ChatRoomDTO;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Getter
@Setter
@Document(value = "chatRoom")
public class ChatRoomDocument {

    @Id
    private String id;
    private int status; // 상태
    private int chatRoomFavorite; // 즐겨찾기
    private int chatRoomReadCnt; // 안읽은메세지수
    private String chatRoomName; // 채팅방이름
    private String leader;  // 방장 uid
    private List<String> members;  // 채팅방 구성원 uid
    private LocalDateTime lastUpdated; // 마지막으로 업데이트된 시간

    private List<ChatMapperDocument> chatMappers;

    public ChatRoomDTO toDTO() {
        return ChatRoomDTO.builder()
                .id(this.id)
                .status(this.status)
                .chatRoomFavorite(this.chatRoomFavorite)
                .chatRoomReadCnt(this.chatRoomReadCnt)
                .chatRoomName(this.chatRoomName)
                .leader(this.leader)
                .members(this.members)
                .lastUpdated(this.lastUpdated)
                .build();
    }
}
