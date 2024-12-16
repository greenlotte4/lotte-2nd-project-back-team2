package com.backend.dto.response.page;

import com.backend.dto.response.user.GetUsersAllDto;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
@Builder
public class MessageUsersDto {
    private List<GetUsersAllDto> selectedUsers;
    private String pageId;
}
