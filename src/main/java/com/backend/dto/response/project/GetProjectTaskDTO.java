package com.backend.dto.response.project;

import com.backend.entity.project.ProjectComment;
import com.backend.entity.project.ProjectSubTask;
import com.backend.entity.project.TaskTag;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetProjectTaskDTO {
    private Long id;

    private Long columnId;

    private String title; // 할일
    private String content; // 세부사항
    private int priority; // 중요도

    private int status; // 완료, 미완료

    private LocalDate duedate; // 마감일
}
