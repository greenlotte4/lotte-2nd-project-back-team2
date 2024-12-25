package com.backend.dto.response.project;

import com.backend.entity.project.ProjectAssign;
import com.backend.entity.project.ProjectColumn;
import com.backend.entity.project.ProjectCoworker;
import com.backend.entity.project.ProjectTask;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetProjectTaskDTO {
    private Long id;

    private Long ProjectId;
    private Long columnId;

    private String title; // 할일
    private String content; // 세부사항
    private String priority; // 중요도

    private int position;
    private int status; // 완료, 미완료

    private LocalDate duedate; // 마감일
    private List<GetProjectSubTaskDTO> subTasks = new ArrayList<>();
    private List<GetProjectCommentDTO> comments = new ArrayList<>();
    private List<GetProjectAssignDTO> assign = new ArrayList<>();
    private List<GetProjectCoworkerDTO> associate = new ArrayList<>();

    public ProjectTask toProjectTask() {
        ProjectTask task = ProjectTask.builder()
                .id(id)
                .column(ProjectColumn.builder().id(columnId).build())
                .title(title)
                .content(content)
                .position(position)
                .priority(priority)
                .duedate(duedate)
                .status(status)
                .subTasks(subTasks.stream().map(GetProjectSubTaskDTO::toEntity).collect(Collectors.toList()))
                .comments(comments.stream().map(GetProjectCommentDTO::toEntity).collect(Collectors.toList()))
                .build();
        if (associate==null) {
            return task;
        } else {
            return this.addAssign(task);
        }
    }
    private ProjectTask addAssign(ProjectTask task) {
        task.setAssign(associate.stream().map(DTO->(
                ProjectAssign.builder()
                        .task(task)
                        .user(DTO.toProjectCoworker())
                        .build()
        )).collect(Collectors.toList()));
        return task;
    }

}
