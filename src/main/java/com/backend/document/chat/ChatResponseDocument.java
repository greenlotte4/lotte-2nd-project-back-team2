package com.backend.document.chat;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(value="chatResponse")
public class ChatResponseDocument {

    @Id
    private String id;

    // 가져온 채팅 메시지 목록
    private List<ChatMessageDocument> messages;
    
    // 추가로 더 불러올 메시지가 있는지 여부
    private boolean hasMore;

}
