package com.backend.entity.project;

import com.backend.dto.response.project.GetProjectDTO;
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
    private Set<ProjectCoworker> coworkers = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private Set<ProjectColumn> columns = new TreeSet<>(Comparator.comparing(ProjectColumn::getPosition));

    @Version // Optimistic locking을 위한 버전 필드
    private Long version;

    @Column(name = "project_progress")
    private Integer projectProgress;

    public void addCoworker(ProjectCoworker coworker) {
        if(coworkers == null) {coworkers = new HashSet<>();}
        coworkers.add(coworker);
        coworker.setProject(this);
    }
    public void removeCoworker(ProjectCoworker coworker) {
        coworkers.remove(coworker);
        coworker.setProject(null);
    }

    public GetProjectDTO toGetProjectDTO() {
        return GetProjectDTO.builder()
                .id(id)
                .title(title)
                .type(type)
                .status(status)
                .columns(columns.stream().map(ProjectColumn::toGetProjectColumnDTO).collect(Collectors.toSet()))
                .coworkers(coworkers.stream().map(ProjectCoworker::toGetCoworkerDTO).collect(Collectors.toSet()))
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

}
