package com.backend.dto.request.project;

import com.backend.dto.response.user.GetUsersAllDto;
import com.backend.entity.project.Project;
import com.backend.entity.project.ProjectCoworker;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostProjectDTO {
    private String title;
    private int type; // 부서내부, 회사내부, 협력, 공개
    private List<GetUsersAllDto> coworkers;

    private List<ProjectCoworker> coworkerEntities;

    public Project toProject() {
        return Project.builder()
                .title(title)
                .type(type)
                .status(1)
                .coworkers(coworkerEntities)
                .build();
    }
}
