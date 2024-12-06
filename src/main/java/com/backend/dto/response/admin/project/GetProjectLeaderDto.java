package com.backend.dto.response.admin.project;

import com.backend.entity.project.Project;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Getter
@Setter
public class GetProjectLeaderDto {
    private String title;
    private String status;
    private String type;
    private String name;
    private String level;
    private String email;
    private Long id;
}
