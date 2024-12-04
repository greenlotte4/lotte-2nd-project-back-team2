package com.backend.entity.project;

import com.backend.dto.response.project.GetProjectDTO;
import com.backend.dto.response.project.GetProjectListDTO;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Builder
@Entity
@Table(name = "project")
public class Project { //프로젝트
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private int type; // 부서내부, 회사내부, 협력, 공개
    private int status; // 대기중, 진행중, 완료, 삭제

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectCoworker> coworkers;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectColumn> columns;

    public void addCoworker(ProjectCoworker coworker) {
        if(this.coworkers == null) {this.coworkers = new ArrayList<>();}
        this.coworkers.add(coworker);
        coworker.setProject(this);
    }

    public GetProjectDTO toGetProjectDTO() {
        return GetProjectDTO.builder()
                .title(title)
                .type(type)
                .status(status)
                .columns(columns.stream().map(ProjectColumn::toGetProjectColumnDTO).toList())
                .coworkers(coworkers.stream().map(ProjectCoworker::toGetCoworkerDTO).toList())
                .build();
    }

}
