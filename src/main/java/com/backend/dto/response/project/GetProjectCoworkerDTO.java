package com.backend.dto.response.project;

import com.backend.entity.project.Project;
import com.backend.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetProjectCoworkerDTO {
    private Long id;

    private String uid;
    private String name;
    private String email;
    private String group;
    private String level;

    private boolean isOwner;

    private boolean canRead; // 읽기 권한
    private boolean canAddTask; // 추가 권한
    private boolean canUpdateTask; // 수정 권한
    private boolean canDeleteTask; // 삭제 권한
    private boolean canEditProject; // 프로젝트 전체 권한




}
