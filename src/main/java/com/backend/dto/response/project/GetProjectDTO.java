package com.backend.dto.response.project;

import com.backend.entity.project.ProjectColumn;
import com.backend.entity.project.ProjectCoworker;
import lombok.*;

import java.util.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetProjectDTO {
    private Long id;

    private String title;
    private int type; // 부서내부, 회사내부, 협력, 공개
    private int status; // 대기중, 진행중, 완료, 삭제

    private List<GetProjectCoworkerDTO> coworkers = new ArrayList<>();
    private List<GetProjectColumnDTO> columns = new ArrayList<>();

}
