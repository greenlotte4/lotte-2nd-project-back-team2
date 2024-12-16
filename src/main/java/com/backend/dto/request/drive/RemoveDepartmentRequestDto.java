package com.backend.dto.request.drive;


import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RemoveDepartmentRequestDto {
    private String id;
    private String type;
    private String ownerId;
    private List<String> deletedDepartments;
}
