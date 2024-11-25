package com.backend.entity.project;

import jakarta.persistence.*;
import lombok.*;

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

    @OneToMany(fetch = FetchType.LAZY)
    private List<ProjectMember> members;

    @OneToMany
    private List<ProjectColumn> columns;

}
