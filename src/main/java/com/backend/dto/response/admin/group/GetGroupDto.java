package com.backend.dto.response.admin.group;

import com.backend.dto.response.user.GetUsersAllDto;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
@Builder
public class GetGroupDto {
    private Long id;
    private String name;
    private String description;
    private List<GetUsersAllDto> users;
    private GetUsersAllDto leader;
    private int link;
}
