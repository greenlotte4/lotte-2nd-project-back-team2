package com.backend.dto.request.drive;


import lombok.*;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDto {

    private String departmentId;
    private String departmentName;
    private int departmentCnt;
    private String permission;
}
