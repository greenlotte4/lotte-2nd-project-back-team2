package com.backend.dto.response.project;

import com.backend.entity.project.Project;
import com.backend.entity.project.ProjectColumn;
import lombok.*;
import java.util.List;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetProjectColumnDTO {
    private Long id;

    private String title;
    private String color;
    private int position;

    private Set<GetProjectTaskDTO> tasks;

    public ProjectColumn toEntityAddProject(Long projectId) {
        return ProjectColumn.builder()
                .id(id)
                .title(title)
                .color(color)
                .position(position)
                .project(Project.builder().id(projectId).build())
                .build();
    }
}