package com.backend.entity.project;

import com.backend.dto.response.project.GetProjectColumnDTO;
import com.backend.dto.response.project.GetProjectTaskDTO;
import jakarta.persistence.*;
import lombok.*;

import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Builder
@Entity
@Table(name = "project_column")
public class ProjectColumn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String color;

    @Setter
    private int position;

    @Setter
    @ToString.Exclude
    @ManyToOne(cascade = CascadeType.PERSIST)
    private Project project;

    @Setter
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "column", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProjectTask> tasks = new ArrayList<>();

    public GetProjectColumnDTO toGetProjectColumnDTO () {
        return GetProjectColumnDTO.builder()
                .id(id)
                .title(title)
                .color(color)
                .position(position)
                .tasks(tasks.stream().map(ProjectTask::toGetProjectTaskDTO).collect(Collectors.toList()))
                .build();
    }

    public void addTask (ProjectTask task) {
            if(tasks==null) {tasks = new ArrayList<>();}

            if(!tasks.contains(task)) {
                tasks.add(task);}
            tasks.add(task);
            task.setColumn(this);
    }
}