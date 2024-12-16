package com.backend.dto.response.user;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
@Builder
public class GetUsersAllDto {
    private Long id;
    private String uid;
    private String name;
    private String email;
    private String authority;
    private String group;
    private String level;
    private String profile;
}
