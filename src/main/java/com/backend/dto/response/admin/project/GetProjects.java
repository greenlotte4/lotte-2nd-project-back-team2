package com.backend.dto.response.admin.project;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
@Builder
public class GetProjects {
    private String projectTitle;
    private String projectStatus;
    private Long projectId;
    private int projectTotalCnt;
    private int projectCompletedCnt;
}
