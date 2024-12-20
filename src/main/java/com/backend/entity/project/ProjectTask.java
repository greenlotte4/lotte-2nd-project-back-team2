package com.backend.entity.project;

import com.backend.dto.response.project.GetProjectCoworkerDTO;
import com.backend.dto.response.project.GetProjectSubTaskDTO;
import com.backend.dto.response.project.GetProjectTaskDTO;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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
    private String priority; // 중요도

    private int status; // 완료, 미완료
    private int position;

    private LocalDate duedate; // 마감일

    @Setter
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "task")
    @Builder.Default
    private List<ProjectSubTask> subTasks = new ArrayList<>();

    @Setter
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "task")
    @Builder.Default
    private List<ProjectComment> comments = new ArrayList<>();

    @Setter
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProjectAssign> assign = new ArrayList<>();

//    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<TaskTag> tags = new ArrayList<>();

    public GetProjectTaskDTO toGetProjectTaskDTO() {
        GetProjectTaskDTO task = GetProjectTaskDTO.builder()
                .id(id)
                .ProjectId(column.getProject().getId())
                .columnId(column.getId())
                .title(title)
                .content(content)
                .priority(priority)
                .status(status)
                .duedate(duedate)
                .comments(comments.stream().map(ProjectComment::toDTO).collect(Collectors.toList()))
                .subTasks(subTasks.stream().map(ProjectSubTask::toDTO).collect(Collectors.toList()))
                .assign(assign.stream().map(ProjectAssign::toDTO).collect(Collectors.toList()))
                .build();
        return task;
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

    public void update(ProjectTask task) {
        if(!this.title.equals(task.title)) this.title = task.title;
        if(!this.content.equals(task.content)) this.content = task.content;
        if(!this.priority.equals(task.priority)) this.priority = task.priority;
        if(!this.duedate.equals(task.duedate))this.duedate = task.duedate;
        // Assign 컬렉션 업데이트
        this.assign.clear();
        if (task.assign != null) {
            for (ProjectAssign newAssign : task.assign) {
                newAssign.setTask(this); // 양방향 연관관계 유지
                this.assign.add(newAssign);
            }
        }
    }

}
