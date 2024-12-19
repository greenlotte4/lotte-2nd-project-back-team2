package com.backend.document.chat;

import com.backend.dto.chat.ChatMessageDTO;
import jakarta.persistence.Id;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(value="chatMessage")
public class ChatMessageDocument {

    @Id
    private String id;
    private String roomId; // 채팅방 아이디
    @Builder.Default
    private int status = 0;  // 상태
    private String type; // 공지 , 일반채팅 , 파일, 입장메시지, 퇴장메시지
    private String content; // 내용
    private String fileUrl; // 파일 Url
    private String sender; // 보낸 사람 아이디
    private String senderName; // 보낸 사람 이름
    @CreationTimestamp
    @Builder.Default
    private LocalDateTime timeStamp = LocalDateTime.now(); // 보낸 시간

    public ChatMessageDTO toDTO() {
        return ChatMessageDTO.builder()
                .id(this.id)
                .roomId(this.roomId)
                .status(this.status)
                .type(this.type)
                .content(this.content)
                .fileUrl(this.fileUrl)
                .sender(this.sender)
                .senderName(this.senderName)
                .timeStamp(this.timeStamp)
                .build();
    }
}

