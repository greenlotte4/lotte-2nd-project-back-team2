package com.backend.service;

import com.backend.dto.request.project.PostProjectDTO;
import com.backend.dto.response.user.GetUsersAllDto;
import com.backend.entity.project.Project;
import com.backend.entity.project.ProjectCoworker;
import com.backend.entity.user.User;
import com.backend.repository.project.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;

    public Project createProject(PostProjectDTO postDTO, User user) {
        List<GetUsersAllDto> userList = postDTO.getCoworkers();
        List<ProjectCoworker> coworkerList = userList.stream().map(u->{
            int level = switch (u.getLevel()) {
                case "사원" -> 1;
                case "주임" -> 2;
                case "대리" -> 3;
                case "과장" -> 4;
                case "차장" -> 5;
                case "부장" -> 6;
                default -> 0;  // Handle unexpected levels
            };
            User newUser = User.builder()
                    .id(u.getId())
                    .uid(u.getUid())
                    .name(u.getName())
                    .email(u.getEmail())
                    .level(level)
                    .build();
            return ProjectCoworker.builder()
                    .user(newUser)
                    .isOwner(false)
                    .build();
        }).collect(Collectors.toList());
        coworkerList.add(ProjectCoworker.builder().user(user).isOwner(true).build());
        postDTO.setCoworkerEntities(coworkerList);
        log.info(postDTO);

        return projectRepository.save(postDTO.toProject());
    }

}
