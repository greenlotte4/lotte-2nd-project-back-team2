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
    private boolean isChecked; // 리스트 체크 여부
    private String name; // 이름

    private Long taskId;
    private Long columnId;
    private Long projectId;

    public ProjectSubTask toEntity() {
        return ProjectSubTask.builder()
                .id(id)
                .isChecked(isChecked)
                .name(name)
                .build();
    }
}