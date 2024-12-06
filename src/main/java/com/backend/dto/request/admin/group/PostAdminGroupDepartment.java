package com.backend.dto.request.admin.group;

import com.backend.dto.response.GetAdminUsersDtailRespDto;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Getter
@Setter
public class PostAdminGroupDepartment {
    private String depName;
    private String depDescription;
    private Long leader;
    private List<Long> users;
    private int link;
}
