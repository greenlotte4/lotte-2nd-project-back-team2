package com.backend.dto.chat;

import com.backend.document.chat.ChatMemberDocument;
import jakarta.persistence.Id;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMemberDTO {

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
    private List<ChatMemberDTO> frequent_members = new ArrayList<>();
    private List<String> roomIds = new ArrayList<>();

    public ChatMemberDocument toDocument() {
        return ChatMemberDocument.builder()
                .uid(this.uid)
                .name(this.name)
                .email(this.email)
                .hp(this.hp)
                .level(this.level)
                .group(this.group)
                .profileUrl(this.profileUrl)
                .profileSName(this.profileSName)
                .frequent_members(this.frequent_members.stream().map(ChatMemberDTO::toDocument).toList())
                .roomIds(this.roomIds)
                .build();
    }
}

