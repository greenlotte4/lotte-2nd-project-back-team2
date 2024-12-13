package com.backend.dto.response.project;

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
    private int priority; // 중요도

    private int status; // 완료, 미완료

    private LocalDate duedate; // 마감일
    private List<GetProjectSubTaskDTO> subTasks = new ArrayList<>();
    private List<GetProjectCommentDTO> comments = new ArrayList<>();
    private List<GetProjectCoworkerDTO> associate = new ArrayList<>();

    public ProjectTask toProjectTask() {
        try{
            return ProjectTask.builder()
                    .id(id)
                    .column(ProjectColumn.builder().id(columnId).build())
                    .title(title)
                    .content(content)
                    .priority(priority)
                    .duedate(duedate)
                    .status(status)
                    .subTasks(subTasks.stream().map(GetProjectSubTaskDTO::toEntity).collect(Collectors.toList()))
                    .comments(comments.stream().map(GetProjectCommentDTO::toEntity).collect(Collectors.toList()))
                    .associate(associate.stream().map(GetProjectCoworkerDTO::toProjectCoworker).collect(Collectors.toList()))
                    .build();
        }catch (Exception e){
            return null;
        }
    }

}
