package com.backend.dto.response.project;

import com.backend.entity.project.ProjectSubTask;
import com.backend.entity.project.ProjectTask;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetProjectSubTaskDTO {

    private Long id;
    private Long taskId;
    private boolean isChecked; // 리스트 체크 여부
    private String name; // 이름

    public ProjectSubTask toEntity() {
        return ProjectSubTask.builder()
                .id(id)
                .task(ProjectTask.builder().id(taskId).build())
                .isChecked(isChecked)
                .name(name)
                .build();
    }
}