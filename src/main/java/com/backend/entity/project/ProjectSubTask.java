package com.backend.entity.project;

import com.backend.dto.response.project.GetProjectSubTaskDTO;
import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Builder
@Entity
@Table(name = "project_sub_task")
public class ProjectSubTask { //Task 내부 체크리스트
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @Setter
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "task_id")
    private ProjectTask task;

    private boolean isChecked; // 리스트 체크 여부
    private String name; // 이름


    public GetProjectSubTaskDTO toDTO(){
        return GetProjectSubTaskDTO.builder()
                .id(id)
                .taskId(task!=null?task.getId():null)
                .columnId(task.getColumn().getId())
                .projectId(task.getColumn().getProject().getId())
                .name(name)
                .isChecked(isChecked)
                .build();
    }

    public void click(){
        isChecked = !isChecked;
    }
}