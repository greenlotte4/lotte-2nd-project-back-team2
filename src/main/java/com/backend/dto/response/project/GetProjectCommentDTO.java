package com.backend.dto.response.project;

import com.backend.dto.response.UserDto;
import com.backend.entity.project.ProjectComment;
import com.backend.entity.user.User;
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

    @ToString.Exclude
    private UserDto user;
    private String writer;

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
                .user(User.builder().id(user.getId()).uid(user.getUid()).name(user.getName()).profileImgPath(user.getProfileImgPath()).build())
                .build();
    }
}