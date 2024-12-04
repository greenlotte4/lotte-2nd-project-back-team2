package com.backend.entity.project;

import com.backend.dto.response.project.GetProjectColumnDTO;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

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

    @ManyToOne
    private Project project;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "columnId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectTask> tasks;

    public GetProjectColumnDTO toGetProjectColumnDTO () {
        return GetProjectColumnDTO.builder()
                .id(id)
                .title(title)
                .color(color)
                .tasks(tasks.stream().map(ProjectTask::toGetProjectTaskDTO).toList())
                .build();
    }
}