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
    private int priority; // 중요도

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
    @OneToMany(cascade = CascadeType.PERSIST, orphanRemoval = true)
    @JoinColumn(name = "task_id")
    @Builder.Default
    private List<ProjectCoworker> associate = new ArrayList<>();

//    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<TaskTag> tags = new ArrayList<>();

    public GetProjectTaskDTO toGetProjectTaskDTO() {
        return GetProjectTaskDTO.builder()
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
                .associate(associate.stream().map(ProjectCoworker::toGetCoworkerDTO).collect(Collectors.toList()))
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

    public void addAssociate(ProjectCoworker asso) {
        if (associate == null) {associate = new ArrayList<>();}
        associate.add(asso);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // 같은 객체 참조인 경우
        if (o == null || getClass() != o.getClass()) return false; // 클래스가 다르거나 null이면 false
        ProjectTask that = (ProjectTask) o;
        return Objects.equals(id, that.id); // id가 동일한 경우 true
    }

    @Override
    public int hashCode() {
        return Objects.hash(id); // id 기반의 해시코드
    }
}
