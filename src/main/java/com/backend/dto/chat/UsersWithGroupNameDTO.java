package com.backend.dto.chat;

import lombok.*;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UsersWithGroupNameDTO {

    private Long id;
    private String uid;
    private String email;
    private String imgPath;
    private String name;
    private String groupName;

}
