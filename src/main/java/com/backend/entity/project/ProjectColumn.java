package com.backend.entity.project;

import com.backend.dto.response.project.GetProjectColumnDTO;
import com.backend.dto.response.project.GetProjectTaskDTO;
import jakarta.persistence.*;
import lombok.*;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
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
    private int position;

    @ManyToOne
    private Project project;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "columnId", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProjectTask> tasks = new TreeSet<>(Comparator.comparing(ProjectTask::getPosition));

    public GetProjectColumnDTO toGetProjectColumnDTO () {
        return GetProjectColumnDTO.builder()
                .id(id)
                .title(title)
                .color(color)
                .position(position)
                .tasks(tasks.stream().map(ProjectTask::toGetProjectTaskDTO).collect(Collectors.toSet()))
                .build();
    }
}