package com.backend.dto.request.project;

import com.backend.dto.response.project.GetProjectColumnDTO;
import com.backend.dto.response.project.GetProjectCoworkerDTO;
import com.backend.dto.response.user.GetUsersAllDto;
import com.backend.entity.project.Project;
import com.backend.entity.project.ProjectCoworker;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostProjectDTO {

    private String title;
    private int type; // 부서내부, 회사내부, 협력, 공개
    private int status; // 0:삭제 1:대기중 2:진행중 3:완료

    private List<GetProjectColumnDTO> columns;
    private List<GetProjectCoworkerDTO> coworkers;
    private List<ProjectCoworker> coworkerEntities;

    public Project toProject() {
        return Project.builder()
                .title(title)
                .type(type)
                .status(1)
                .columns(columns.stream().map(GetProjectColumnDTO::toEntity).collect(Collectors.toList()))
                .coworkers(coworkerEntities)
                .build();
    }
}
