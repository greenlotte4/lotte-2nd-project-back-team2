package com.backend.dto.response.admin.user;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
@Builder
public class GetGroupUsersDto {
    private Long id;
    private String name;
    private String state;
    private String attendance;
    private String level;
    private String createAt;
}
