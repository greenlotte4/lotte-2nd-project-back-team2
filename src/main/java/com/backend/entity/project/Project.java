package com.backend.entity.project;

import com.backend.dto.response.admin.project.GetProjects;
import com.backend.dto.response.project.GetProjectDTO;
import com.backend.dto.response.project.GetProjectListDTO;
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
@Table(name = "project")
public class Project { //프로젝트
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private int type; // 1:부서내부 2:회사내부 3:협력 4:팀 5:공개
    private int status; // 0:삭제 1:대기중 2:진행중 3:완료

    @Setter
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<ProjectCoworker> coworkers = new ArrayList<>();

    @Setter
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<ProjectColumn> columns = new ArrayList<>();

    @Column(name = "project_progress")
    private Integer projectProgress;

    public void addCoworker(ProjectCoworker coworker) {
        if(coworkers == null) {coworkers = new ArrayList<>();}
        coworkers.add(coworker);
        coworker.setProject(this);
    }
    public void removeCoworker(ProjectCoworker coworker) {
        coworkers.remove(coworker);
        coworker.setProject(null);
    }

    public void addColumn(ProjectColumn column) {
        if(columns == null) {columns = new ArrayList<>();}
        columns.add(column);
        column.setProject(this);
    }

    public GetProjectDTO toGetProjectDTO() {
        return GetProjectDTO.builder()
                .id(id)
                .title(title)
                .type(type)
                .status(status)
                .columns(columns.stream().map(ProjectColumn::toGetProjectColumnDTO).collect(Collectors.toList()))
                .coworkers(coworkers.stream().map(ProjectCoworker::toGetCoworkerDTO).collect(Collectors.toList()))
                .build();
    }

    public String selectStatus(){
        return switch (status) {
            case 1 -> "대기중";
            case 2 -> "진행중";
            default -> "완료";
        };
    }

    public String selectType(){
        return switch (type) {
            case 1 -> "부서";
            case 2 -> "회사";
            case 3 -> "협력";
            case 4 -> "팀";
            default -> "공개";
        };
    }

    public GetProjects toGetProjects() {
        return GetProjects.builder()
                .projectTitle(title)
                .projectStatus(selectStatus())
                .projectId(id)
                .build();
    }

}
