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
    @ManyToOne(cascade = CascadeType.ALL)
    @ToString.Exclude
    private Project project;

    @Setter
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "column", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectTask> tasks = new ArrayList<>();

    public GetProjectColumnDTO toGetProjectColumnDTO () {
        return GetProjectColumnDTO.builder()
                .id(id)
                .title(title)
                .color(color)
                .position(position)
                .tasks(tasks.stream().map(ProjectTask::toGetProjectTaskDTO).toList())
                .build();
    }

    public void addTask (ProjectTask task) {
        if(tasks==null) {tasks = new ArrayList<>();}
        tasks.add(task);
        task.setColumn(this);
    }
}