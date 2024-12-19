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

    private String user_id;

    @ToString.Exclude
    private GetProjectTaskDTO task;

    private User user;

    private String content;

    private Long taskId;
    private Long projectId;

    @CreationTimestamp
    private LocalDateTime rdate;

    public ProjectComment toEntity() {
        return ProjectComment.builder()
                .id(id)
                .content(content)
                .rdate(rdate)
                .user_id(user_id)
                .build();
    }
}