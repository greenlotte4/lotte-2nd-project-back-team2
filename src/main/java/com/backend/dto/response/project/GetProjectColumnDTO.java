package com.backend.dto.response.project;

import com.backend.entity.project.Project;
import com.backend.entity.project.ProjectColumn;
import lombok.*;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetProjectColumnDTO {
    private Long id;
    private Long projectId;

    private String title;
    private String color;
    private int role;
    private int position;

    private List<GetProjectTaskDTO> tasks;

    public ProjectColumn toEntityAddProject(Long projectId) {
        return ProjectColumn.builder()
                .id(id)
                .title(title)
                .color(color)
                .role(role)
                .position(position)
                .project(Project.builder().id(projectId).build())
                .build();
    }
    public ProjectColumn toEntity() {
        return ProjectColumn.builder()
                .id(id)
                .title(title)
                .color(color)
                .role(role)
                .position(position)
                .tasks(tasks.stream().map(GetProjectTaskDTO::toProjectTask).collect(Collectors.toList()))
                .build();
    }
}