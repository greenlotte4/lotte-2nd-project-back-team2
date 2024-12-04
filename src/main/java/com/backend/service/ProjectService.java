package com.backend.service;

import com.backend.dto.request.project.PostProjectDTO;
import com.backend.dto.response.project.GetProjectDTO;
import com.backend.dto.response.user.GetUsersAllDto;
import com.backend.entity.project.Project;
import com.backend.entity.project.ProjectCoworker;
import com.backend.entity.user.User;
import com.backend.repository.project.ProjectCoworkerRepository;
import com.backend.repository.project.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


/*

    이름 : 김주경
    날짜 : 2024/12/03
    작업내용 : 프로젝트 생성

    수정이력
        - 2024/12/04 김주경 - 코드 간편화

 */

@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class ProjectService {
    private final UserService userService;
    private final ProjectRepository projectRepository;
    private final ProjectCoworkerRepository coworkerRepository;

    public Project createProject(PostProjectDTO postDTO, String username) {

        List<GetUsersAllDto> userList = postDTO.getCoworkers();
        Project project = postDTO.toProject();

        userList.forEach(u -> project.addCoworker(ProjectCoworker.builder()
                .user(User.builder().id(u.getId()).build())
                .project(project)
                .isOwner(false)
                .build()));

        project.addCoworker(ProjectCoworker.builder()
                        .user(userService.getUserByuid(username))
                        .isOwner(true)
                        .build());

        return projectRepository.save(project);
    }

    public Map<String,Object> getAllProjects(String username) {
        Map<String,Object> map = new HashMap<>();

        List<ProjectCoworker> allProjects = coworkerRepository.findByUserAndProjectStatusIsNot(userService.getUserByuid(username), 0);

        Map<String, List<ProjectCoworker>> groupedByStatus = allProjects.stream()
                .collect(Collectors.groupingBy(pc -> {
                    int status = pc.getProject().getStatus();
                    if (status == 1) return "waiting";
                    else if (status == 2) return "inProgress";
                    else if (status == 3) return "completed";
                    else return "unknown";
                }));

        map.put("waiting", groupedByStatus.getOrDefault("waiting", Collections.emptyList()).stream().map(ProjectCoworker::toGetProjectListDTO).toList());
        map.put("inProgress", groupedByStatus.getOrDefault("inProgress", Collections.emptyList()).stream().map(ProjectCoworker::toGetProjectListDTO).toList());
        map.put("completed", groupedByStatus.getOrDefault("completed", Collections.emptyList()).stream().map(ProjectCoworker::toGetProjectListDTO).toList());
        map.put("count",allProjects.size());
        return map;
    }

    public GetProjectDTO getProject(Long projectId) {
        Optional<Project> optProject = projectRepository.findById(projectId);
        if (optProject.isPresent()) {
            Project project = optProject.get();
            return project.toGetProjectDTO();
        }
        return null;
    }

}
