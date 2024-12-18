package com.backend.entity.project;

import com.backend.dto.response.project.GetProjectCoworkerDTO;
import com.backend.dto.response.project.GetProjectListDTO;
import com.backend.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.PersistenceCreator;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Builder
@Entity
@Table(name = "project_coworker")
public class ProjectCoworker { //프로젝트별 멤버 권한
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private boolean isOwner;

    private boolean canRead; // 읽기 권한
    private boolean canAddTask; // 추가 권한
    private boolean canUpdateTask; // 수정 권한
    private boolean canDeleteTask; // 삭제 권한
    private boolean canEditProject; // 프로젝트 전체 권한

    @PrePersist
    private void setBasicPermit(){
        if (isOwner){
            canRead = true;
            canAddTask = true;
            canUpdateTask = true;
            canDeleteTask = true;
            canEditProject = true;
        }else {
            canRead = true;
            canAddTask = true;
            canUpdateTask = true;
            canDeleteTask = true;
            canEditProject = false;
        }
    }


    public GetProjectListDTO toGetProjectListDTO() {
        return GetProjectListDTO.builder()
                .id(project.getId())
                .title(project.getTitle())
                .isOwner(isOwner)
                .build();
    }

    public GetProjectCoworkerDTO toGetCoworkerDTO() {
        String group;
        if(user.getGroupMappers()!=null&&!user.getGroupMappers().isEmpty()){
            group = user.getGroupMappers().get(0).getGroup().getName();
        } else {
            group = "소속없음";
        }
        return GetProjectCoworkerDTO.builder()
                .id(user.getId())
                .uid(user.getUid())
                .name(user.getName())
                .email(user.getEmail())
                .group(group)
                .level(user.getLevel()!=null?user.selectLevelString():null)
                .canRead(canRead)
                .canAddTask(canAddTask)
                .canUpdateTask(canUpdateTask)
                .canDeleteTask(canDeleteTask)
                .canEditProject(canEditProject)
                .build();
    }

}
