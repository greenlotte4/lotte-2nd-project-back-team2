package com.backend.service;

import com.backend.dto.request.project.PatchCoworkersDTO;
import com.backend.dto.request.project.PostProjectDTO;
import com.backend.dto.response.admin.project.GetProjectLeaderDetailDto;
import com.backend.dto.response.admin.project.GetProjectLeaderDto;
import com.backend.dto.response.admin.project.GetProjects;
import com.backend.dto.response.project.GetProjectColumnDTO;
import com.backend.dto.response.project.GetProjectDTO;
import com.backend.dto.response.project.GetProjectTaskDTO;
import com.backend.dto.response.user.GetUsersAllDto;
import com.backend.entity.calendar.CalendarMapper;
import com.backend.entity.group.Group;
import com.backend.entity.group.GroupLeader;
import com.backend.entity.project.Project;
import com.backend.entity.project.ProjectColumn;
import com.backend.entity.project.ProjectCoworker;
import com.backend.entity.project.ProjectTask;
import com.backend.entity.user.User;
import com.backend.repository.GroupLeaderRepository;
import com.backend.repository.GroupRepository;
import com.backend.repository.UserRepository;
import com.backend.repository.calendar.CalendarMapperRepository;
import com.backend.repository.project.ProjectColumnRepository;
import com.backend.repository.project.ProjectCoworkerRepository;
import com.backend.repository.project.ProjectRepository;
import com.backend.repository.project.ProjectTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


/*

    이름 : 김주경
    날짜 : 2024/12/03
    작업내용 : 프로젝트 생성

    수정이력
        - 2024/12/04 김주경 - 코드 간편화, 프로젝트 불러오기
        - 2024/12/05 김주경 - 프로젝트 컬럼 추가, 수정
        - 2024/12/06 김주경 - 작업자 수정

 */

@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class ProjectService {
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectCoworkerRepository coworkerRepository;
    private final GroupRepository groupRepository;
    private final GroupLeaderRepository groupLeaderRepository;
    private final CalendarMapperRepository calendarMapperRepository;
    private final ProjectColumnRepository columnRepository;
    private final ProjectTaskRepository taskRepository;

    public Project createProject(PostProjectDTO postDTO, String username) {

        Set<GetUsersAllDto> userList = postDTO.getCoworkers();
        Project project = postDTO.toProject();

        userList.forEach(u -> project.addCoworker(ProjectCoworker.builder()
                .user(User.builder().id(u.getId()).build())
                .project(project)
                .isOwner(false)
                .build()));

        project.addCoworker(ProjectCoworker.builder()
                        .user(userRepository.findByUid(username).orElseThrow(() -> new IllegalArgumentException(username+"작업자를 찾을 수 없습니다.")))
                        .isOwner(true)
                        .build());

        return projectRepository.save(project);
    }

    public Map<String,Object> getAllProjects(String username) {
        Map<String,Object> map = new HashMap<>();

        List<ProjectCoworker> allProjects
                = coworkerRepository.findByUserAndProjectStatusIsNot(
                        userRepository.findByUid(username)
                            .orElseThrow(
                                    () -> new IllegalArgumentException(username+"작업자를 찾을 수 없습니다.")
                            ), 0);

        Map<String, List<ProjectCoworker>> groupedByStatus = allProjects.stream()
                .collect(Collectors.groupingBy(pc -> {
                    int status = pc.getProject().getStatus();
                    if (status == 1) return "waiting";
                    else if (status == 2) return "inProgress";
                    else if (status == 3) return "completed";
                    else return "unknown";
                }));

        map.put("waiting", groupedByStatus.getOrDefault("waiting", Collections.emptyList()).stream().map(ProjectCoworker::toGetProjectListDTO).collect(Collectors.toSet()));
        map.put("inProgress", groupedByStatus.getOrDefault("inProgress", Collections.emptyList()).stream().map(ProjectCoworker::toGetProjectListDTO).collect(Collectors.toSet()));
        map.put("completed", groupedByStatus.getOrDefault("completed", Collections.emptyList()).stream().map(ProjectCoworker::toGetProjectListDTO).collect(Collectors.toSet()));
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

    public ResponseEntity<?> getLeaderInfo(String group, String company) {
        Optional<Group> optGroup = groupRepository.findByNameAndStatusIsNotAndCompany(group,0,company);
        if(optGroup.isEmpty()) {
            return ResponseEntity.badRequest().body("정보가 일치하지 않습니다...");
        }
        User leader = optGroup.get().getGroupLeader().getUser();
        List<Project> project = projectRepository.findAllByCoworkers_UserAndStatusIsNot(leader,0);
        GetProjectLeaderDto projectLeaderDto = GetProjectLeaderDto.builder()
                .email(leader.getEmail())
                .name(leader.getName())
                .level(leader.selectLevelString())
                .id(leader.getId())
                .build();

        if(project.isEmpty()){
            projectLeaderDto.setTitle("없음");
            projectLeaderDto.setType("없음");
            projectLeaderDto.setStatus("없음");
        } else {
            if (project.size() >= 2) {
                projectLeaderDto.setTitle(project.get(0).getTitle() + " 외 " + (project.size()-1) + "개");
            } else {
                projectLeaderDto.setTitle(project.get(0).getTitle());
            }
            projectLeaderDto.setType(project.get(0).selectType());
            projectLeaderDto.setStatus(project.get(0).selectStatus());
        }
        return ResponseEntity.ok(projectLeaderDto);
    }

    public ResponseEntity<?> getLeaderDetail(Long id) {
        GetProjectLeaderDetailDto getProjectLeaderDetailDto = new GetProjectLeaderDetailDto();
        Optional<User> leader = userRepository.findById(id);
        if(leader.isEmpty()){
            return ResponseEntity.badRequest().body("로그인 정보가 일치하지 않습니다...");
        }
        getProjectLeaderDetailDto.setHp(leader.get().getHp());
        getProjectLeaderDetailDto.setId(leader.get().getId());
        getProjectLeaderDetailDto.setAddress(leader.get().getAddr1()+" "+leader.get().getAddr2());
        getProjectLeaderDetailDto.setName(leader.get().getName());
        Optional<GroupLeader> groupLeader = groupLeaderRepository.findByUser(leader.get());
        Group group = groupLeader.get().getGroup();
        System.out.println("===========ddd=======");
        System.out.println(groupLeader.get());
        System.out.println(group);
        Optional<CalendarMapper> groupCalendar;
        if(group.getType()==0){
            groupCalendar = calendarMapperRepository.findByUserAndCalendar_Status(leader.get(),3);
        } else {
            groupCalendar = calendarMapperRepository.findByUserAndCalendar_Status(leader.get(),4);
        }
        if(groupCalendar.isPresent()){
            getProjectLeaderDetailDto.setCalendarId(groupCalendar.get().getCalendar().getCalendarId());
            getProjectLeaderDetailDto.setCalendarName(groupCalendar.get().getCalendar().getName());
        } else {
            getProjectLeaderDetailDto.setCalendarId(0L);
            getProjectLeaderDetailDto.setCalendarName("없음");
        }

        List<Project> project = projectRepository.findAllByCoworkers_UserAndStatusIsNot(leader.get(),0);
        if(project.isEmpty()){
            List<GetProjects> projects = new ArrayList<>();
            GetProjects proj = GetProjects.builder()
                    .projectId(0L)
                    .projectStatus("없음")
                    .projectTitle("없음")
                    .build();
            projects.add(proj);
            getProjectLeaderDetailDto.setProjects(projects);
        } else {
            List<GetProjects> projects = new ArrayList<>();
            for (Project project1 : project) {
                GetProjects proj = GetProjects.builder()
                        .projectId(project1.getId())
                        .projectStatus(project1.selectStatus())
                        .projectTitle(project1.getTitle())
                        .build();
                projects.add(proj);
            }
            getProjectLeaderDetailDto.setProjects(projects);
        }

        return ResponseEntity.ok(getProjectLeaderDetailDto);
    }

    public ProjectColumn addColumn(GetProjectColumnDTO columnDTO, Long projectId) {
        return columnRepository.save(columnDTO.toEntityAddProject(projectId));
    }

    public ProjectTask saveTask(GetProjectTaskDTO taskDTO) {
        return taskRepository.save(taskDTO.toProjectTask());
    }

    public void updateCoworkers(PatchCoworkersDTO dto) {
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("해당 프로젝트를 찾을 수 없습니다."));

        if (dto.getRemovedCoworkers() != null) {
            dto.getRemovedCoworkers().forEach(userId -> {
                project.getCoworkers().stream()
                        .filter(coworker -> coworker.getUser().getId().equals(userId))
                        .findFirst()
                        .ifPresent(project::removeCoworker);
            });
        }

        if (dto.getAddedCoworkers() != null) {
            dto.getAddedCoworkers().forEach(userId -> {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("작업자를 찾을 수 없습니다. ID: " + userId));
                project.addCoworker(ProjectCoworker.builder()
                        .user(user)
                        .project(project)
                        .isOwner(false)
                        .build());
            });
        }

    }
}
