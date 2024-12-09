package com.backend.entity.project;

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
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "task_id")
    private ProjectTask task;

    private boolean isChecked; // 리스트 체크 여부
    private String name; // 이름
}