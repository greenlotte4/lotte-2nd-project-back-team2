package com.backend.controller;

import com.backend.dto.request.project.PatchCoworkersDTO;
import com.backend.dto.request.project.PostProjectDTO;
import com.backend.dto.response.project.*;
import com.backend.entity.project.Project;
import com.backend.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Log4j2
@RequestMapping("/api")
@RequiredArgsConstructor
@RestController
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping("/project") // 프로젝트 생성
    public ResponseEntity<?> createProject(@RequestBody PostProjectDTO dto, HttpServletRequest request) {
        String ownerUid = (String) request.getAttribute("uid");
        Project savedProject = projectService.createProject(dto, ownerUid);
        return ResponseEntity.ok().body(savedProject.toGetProjectDTO());
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

    @DeleteMapping("/project/{id}")
    public void deleteProject(@PathVariable Long id) {
        projectService.delete("project", id);
        projectService.sendBoardUpdate(id, "PROJECT_DELETED", id);
    }

    @PatchMapping("/project/coworkers") // 프로젝트 작업자 목록 수정
    public void updateCoworkers(@RequestBody PatchCoworkersDTO dto) {
        projectService.updateCoworkers(dto);
        projectService.sendBoardUpdate(dto.getProjectId(), "COWORKER_UPDATED", dto);
    }


    @MessageMapping("/project/{projectId}/column/{type}")
    public void column(@DestinationVariable Long projectId,
                             @DestinationVariable String type,
                             @Payload GetProjectColumnDTO dto) {
        GetProjectColumnDTO saved = dto;

        if(type.equals("added")){               // 컬럼 생성
            saved = projectService.addColumn(dto, projectId);
        } else if (type.equals("updated")) {    // 컬럼 수정

        } else{                                 // 컬럼 삭제
            projectService.delete("column", dto.getId());
        }

        projectService.sendBoardUpdate(projectId, "COLUMN_"+type.toUpperCase(), saved);
    }

    @MessageMapping("/project/{projectId}/task/{type}")
    public void task(@DestinationVariable Long projectId,
                           @DestinationVariable String type,
                           @Payload GetProjectTaskDTO dto) {
        GetProjectTaskDTO saved = dto;

        if (type.equals("deleted")) {       // 태스크 삭제
            projectService.delete("task", dto.getId());
        } else {                            // 태스크 생성, 수정
            saved = projectService.saveTask(dto);
        }

        projectService.sendBoardUpdate(projectId, "TASK_"+type.toUpperCase(), saved);
    }

    @MessageMapping("/project/{projectId}/sub/{type}")
    public void subTask(@DestinationVariable Long projectId,
                              @DestinationVariable String type,
                              @Payload GetProjectSubTaskDTO dto){
        GetProjectSubTaskDTO saved = dto;

        if(type.equals("added")){               // 서브태스크 생성
            saved = projectService.insertSubTask(dto);
        } else if (type.equals("updated")) {    //서브태스크 수정
            projectService.clickSubTask(dto.getId());
        } else{                                 // 서브태스크 삭제
            projectService.delete("subtask", dto.getId());
        }

        projectService.sendBoardUpdate(projectId, "SUBTASK_"+type.toUpperCase(),  saved);
    }



}
