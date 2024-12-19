package com.backend.dto.chat;

import com.backend.document.chat.ChatMessageDocument;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDTO {

    @Id
    private String id;
    private String roomId; // 채팅방 아이디
    private int status;  // 상태
    private String type; // 공지 ,, 일반채팅 , 파일,
    private String content; // 내용
    private String fileUrl; // 파일 Url
    private String sender; // 보낸 사람 아이디
    private String senderName; // 보낸 사람 이름
    private LocalDateTime timeStamp; // 보낸 시간
    private long count;

    public ChatMessageDocument toDocument() {
        return ChatMessageDocument.builder()
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

