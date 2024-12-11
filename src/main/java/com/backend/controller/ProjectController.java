package com.backend.controller;

import com.backend.dto.request.project.PatchCoworkersDTO;
import com.backend.dto.request.project.PostProjectDTO;
import com.backend.dto.response.project.GetProjectColumnDTO;
import com.backend.dto.response.project.GetProjectDTO;
import com.backend.dto.response.project.GetProjectTaskDTO;
import com.backend.entity.group.Group;
import com.backend.entity.project.Project;
import com.backend.entity.project.ProjectColumn;
import com.backend.entity.project.ProjectTask;
import com.backend.service.ProjectService;
import com.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Log4j2
@RequestMapping("/api")
@RequiredArgsConstructor
@RestController
public class ProjectController {

    private final ProjectService projectService;
    private final UserService userService;

    @PostMapping("/project") // 프로젝트 생성
    public ResponseEntity<?> createProject(@RequestBody PostProjectDTO dto, HttpServletRequest request) {
        String ownerUid = (String) request.getAttribute("uid");
        Project savedProject = projectService.createProject(dto, ownerUid);
        return ResponseEntity.ok().body(savedProject);
    }

    @GetMapping("/projects") // 프로젝트 목록 출력
    public ResponseEntity<?> readProjectList(HttpServletRequest request) {
        String ownerUid = (String) request.getAttribute("uid");
        Map<String,Object> map = projectService.getAllProjects(ownerUid);
        return ResponseEntity.ok().body(map);
    }

    @GetMapping("/project/{id}") // 프로젝트 페이지 출력
    public ResponseEntity<?> readProject(@PathVariable Long id) {
        GetProjectDTO dto = projectService.getProject(id);
        return ResponseEntity.ok().body(dto);
    }

    @PostMapping("/project/{id}") // 프로젝트 컬럼 생성
    public ResponseEntity<?> createColumn(@PathVariable Long id, @RequestBody GetProjectColumnDTO dto) {
        ProjectColumn column = projectService.addColumn(dto, id);
        return ResponseEntity.ok().body(column);
    }
    @RequestMapping(value = "/project/task", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<?> upsertTask(@RequestBody GetProjectTaskDTO dto) { // 태스크 생성, 수정
        log.info("GetProjectTaskDTO: {}",dto);
        ProjectTask task = projectService.saveTask(dto);
        return ResponseEntity.ok().body(task);
    }
    @PatchMapping("/project/coworkers") // 프로젝트 작업자 목록 수정
    public ResponseEntity<String> updateCoworkers(@RequestBody PatchCoworkersDTO dto) {
        try {
            projectService.updateCoworkers(dto);
            return ResponseEntity.ok("작업자 목록이 성공적으로 수정되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("작업자 목록 수정 중 오류가 발생했습니다.");
        }
    }
    @GetMapping("/project/user/group") // 로그인한 유저 부서정보 추출
    public ResponseEntity<?> getUserGroup(HttpServletRequest req){
        log.info("=====================================================여기 요청 들어오나");
        String uid = (String) req.getAttribute("username");
        log.info("uid:"+uid);
        List<Group> groupList = userService.getGroupsByUserUid(uid);
        return ResponseEntity.ok(groupList);
    }

    @DeleteMapping("/project/{type}/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable String type, @PathVariable Long id) {
        log.info("delete "+type+" id : "+id);
        projectService.delete(type, id);
        return ResponseEntity.ok().build();
    }

}
