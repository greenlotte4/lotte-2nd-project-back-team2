package com.backend.dto.response.project;

import com.backend.entity.project.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReqHomeProjectDTO {
    private Long projectId;
    private String projectName;
//    private String projectDescription;

    private List<GetProjectColumnDTO> getProjectColumn;
//    private String columnId;
//    private String columnName;
//    private String columnColor;

}
