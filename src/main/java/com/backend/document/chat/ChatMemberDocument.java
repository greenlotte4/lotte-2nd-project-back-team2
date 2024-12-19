package com.backend.document.chat;

import com.backend.dto.chat.ChatMemberDTO;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Document(value = "chatMember")
public class ChatMemberDocument {
    @Id
    private String id;
    private String uid;
    private String name;
    private String email;
    private String hp;
    private Integer level;
    private String group;
    private String profileUrl;
    private String profileSName;
    @Builder.Default
    private List<ChatMemberDocument> frequent_members = new ArrayList<>();
    @Builder.Default
    private List<String> roomIds = new ArrayList<>();

    private List<ChatMapperDocument> chatMappers;

    public ChatMemberDTO toDTO() {
        return ChatMemberDTO.builder()
                .id(this.id)
                .uid(this.uid)
                .name(this.name)
                .email(this.email)
                .hp(this.hp)
                .level(this.level)
                .group(this.group)
                .profileUrl(this.profileUrl)
                .profileSName(this.profileSName)
                .frequent_members(this.frequent_members.stream().map(ChatMemberDocument::toDTO).collect(Collectors.toList()))
                .roomIds(this.roomIds)
                .build();
    }
}
