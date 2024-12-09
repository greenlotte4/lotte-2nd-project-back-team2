package com.backend.entity.project;

import com.backend.dto.response.project.GetProjectSubTaskDTO;
import com.backend.dto.response.project.GetProjectTaskDTO;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.*;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Builder
@Entity
@Table(name = "project_task")
public class ProjectTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ToString.Exclude
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "column_id")
    private ProjectColumn column;

    private String title; // 할일
    private String content; // 세부사항
    private int priority; // 중요도

    private int status; // 완료, 미완료
    private int position;

    private LocalDate duedate; // 마감일

    @OneToMany
    private List<ProjectSubTask> subTasks = new ArrayList<>();

    @OneToMany
    private List<ProjectComment> comments = new ArrayList<>();

//    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<TaskTag> tags = new ArrayList<>();

    public GetProjectTaskDTO toGetProjectTaskDTO() {
        return GetProjectTaskDTO.builder()
                .id(id)
                .columnId(column.getId())
                .title(title)
                .content(content)
                .priority(priority)
                .status(status)
                .duedate(duedate)
                .build();
    }

    public void addSubTask(ProjectSubTask subtask) {
        if (subTasks == null) {subTasks = new ArrayList<>();}
        subTasks.add(subtask);
        subtask.setTask(this);
    }
    public void addComment(ProjectComment comment) {
        if (comments == null) {comments = new ArrayList<>();}
        comments.add(comment);
        comment.setTask(this);
    }
}
