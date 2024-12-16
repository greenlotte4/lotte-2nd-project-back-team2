package com.backend.dto.request.drive;


import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareRequestDto {
    private String userType;
    private List<SharedUser> sharedUsers;
    private List<DepartmentDto> departments;



}
