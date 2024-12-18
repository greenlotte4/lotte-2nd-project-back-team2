package com.backend.document.chat;

import com.backend.dto.chat.ChatMapperDTO;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(value="chatMapper")
public class ChatMapperDocument {

    @Id
    private String id;

    private String userId;
    private String chatRoomId;
    @Builder.Default
    private LocalDateTime lastReadTimeStamp = LocalDateTime.now(); // 사용자가 마지막으로 읽은 메시지 시간
    private LocalDateTime joinedAt; // 사용자가 채팅방에 가입한 시간
    @Builder.Default
    private int isFrequent = 0; // 즐겨찾기 여부

    public ChatMapperDTO toDTO() {
        return ChatMapperDTO.builder()
                .id(this.id)
                .chatRoomId(this.chatRoomId)
                .userId(this.userId)
                .lastReadTimeStamp(this.lastReadTimeStamp)
                .joinedAt(this.joinedAt)
                .isFrequent(this.isFrequent)
                .build();
    }

}
