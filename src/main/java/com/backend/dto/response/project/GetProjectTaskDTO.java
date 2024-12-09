package com.backend.dto.response.project;

import com.backend.entity.project.ProjectColumn;
import com.backend.entity.project.ProjectTask;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetProjectTaskDTO {
    private Long id;

    private Long columnId;

    private String title; // 할일
    private String content; // 세부사항
    private int priority; // 중요도

    private int status; // 완료, 미완료

    private LocalDate duedate; // 마감일
    private List<GetProjectSubTaskDTO> subtasks;
    private List<GetProjectCommentDTO> comments;

    public ProjectTask toProjectTask() {
        return ProjectTask.builder()
                .id(id)
                .column(ProjectColumn.builder().id(columnId).build())
                .title(title)
                .content(content)
                .priority(priority)
                .duedate(duedate)
                .status(status)
                .comments(null)
                .build();
    }

}
