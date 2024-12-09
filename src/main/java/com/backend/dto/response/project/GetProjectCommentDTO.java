package com.backend.dto.response.project;

import com.backend.entity.project.ProjectComment;
import com.backend.entity.project.ProjectTask;
import com.backend.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetProjectCommentDTO {
    private Long id;

    private GetProjectTaskDTO task;

    private User user;

    private String content;

    @CreationTimestamp
    private LocalDateTime rdate;

    public ProjectComment toEntity() {
        return ProjectComment.builder()
                .id(id)
                .task(task.toProjectTask())
                .build();
    }
}