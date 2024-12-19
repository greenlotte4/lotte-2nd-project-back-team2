package com.backend.entity.project;

import com.backend.dto.response.project.GetProjectAssignDTO;
import com.backend.dto.response.project.GetProjectCoworkerDTO;
import com.backend.dto.response.project.GetProjectListDTO;
import com.backend.dto.response.project.GetProjectTaskDTO;
import com.backend.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Builder
@Entity
@Table(name = "project_assign")
public class ProjectAssign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ToString.Exclude
    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinColumn(name = "project_task")
    private ProjectTask task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coworker_id")
    private ProjectCoworker user;

    public GetProjectAssignDTO toDTO() {
        return GetProjectAssignDTO.builder()
                .id(id)
                .task(GetProjectTaskDTO.builder().id(task.getId()).build())
                .user(user.toGetCoworkerDTO())
                .build();
    }

}
