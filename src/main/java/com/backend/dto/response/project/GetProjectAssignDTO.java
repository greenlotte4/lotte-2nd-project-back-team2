package com.backend.dto.response.project;

import com.backend.entity.project.ProjectAssign;
import com.backend.entity.project.ProjectCoworker;
import com.backend.entity.project.ProjectTask;
import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetProjectAssignDTO {
    private Long id;

    @ToString.Exclude
    private GetProjectTaskDTO task;
    private GetProjectCoworkerDTO user;

    public ProjectAssign toEntity() {
        return ProjectAssign.builder()
                .id(id)
                .task(task.toProjectTask())
                .user(user.toProjectCoworker())
                .build();
    }
}
