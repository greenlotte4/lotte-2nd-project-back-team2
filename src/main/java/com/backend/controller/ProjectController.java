package com.backend.controller;

import com.backend.dto.request.project.PatchCoworkersDTO;
import com.backend.dto.request.project.PostProjectDTO;
import com.backend.dto.response.project.*;
import com.backend.entity.project.Project;
import com.backend.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
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
        return ResponseEntity.ok().body(savedProject.getId());
    }

    @GetMapping("/projects") // 프로젝트 목록 출력
    public ResponseEntity<?> readProjectList(HttpServletRequest request) {
        String ownerUid = (String) request.getAttribute("uid");
        Map<String,Object> map = projectService.getAllProjects(ownerUid);
        return ResponseEntity.ok().body(map);
    }

    @GetMapping("/project/{projectId}") // 프로젝트 페이지 출력
    public ResponseEntity<?> getProject(@PathVariable Long projectId) {
        GetProjectDTO dto = projectService.getProject(projectId);
        return ResponseEntity.ok().body(dto);
    }
    @PutMapping("/project/{projectId}") // 프로젝트 페이지 출력
    public ResponseEntity<?> putProject(@PathVariable Long projectId, @RequestBody GetProjectDTO dto) {
        dto.setId(projectId);
        GetProjectDTO updatedProject = projectService.putProject(dto);
        return ResponseEntity.ok().body(updatedProject);
    }

    @GetMapping("/project/{projectId}/column") // 프로젝트 페이지 출력
    public ResponseEntity<?> getColumn(@PathVariable Long projectId) {
        List<GetProjectColumnDTO> columnList = projectService.getColumns(projectId);
        return ResponseEntity.ok().body(columnList);
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

        if(type.equals("added")){               // 태스크 생성
            saved = projectService.addTask(dto);
        } else if (type.equals("updated")) {    // 태스크 수정
            saved = projectService.updateTask(dto);
        } else{                                 // 태스크 삭제
            projectService.delete("task", dto.getId());
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
            saved = projectService.clickSubTask(dto.getId());
        } else{                                 // 서브태스크 삭제
            projectService.delete("subtask", dto.getId());
        }

        projectService.sendBoardUpdate(projectId, "SUBTASK_"+type.toUpperCase(),  saved);
    }
    @MessageMapping("/project/{projectId}/comment/{type}")
    public void comment(@DestinationVariable Long projectId,
                        @DestinationVariable String type,
                        @Payload GetProjectCommentDTO dto) {
        GetProjectCommentDTO saved = dto;

        if (type.equals("deleted")) {           // 댓글 삭제
            projectService.delete("comment", dto.getId());
        } else if (type.equals("added")) {      // 댓글 생성
            saved = projectService.addComment(dto);
        } else {
            throw new IllegalArgumentException("Unsupported comment type: " + type);
        }

        projectService.sendBoardUpdate(projectId, "COMMENT_" + type.toUpperCase(), saved);
    }

    @GetMapping("/homeProject")
    public ResponseEntity<?> homeProject (Authentication auth){
        Long userId = Long.valueOf(auth.getName());
        List<ReqHomeProjectDTO> dtos = projectService.getHomeProject(userId);
        if(dtos.isEmpty()){
            return ResponseEntity.ok().body("데이터가 없습니다...");
        }
        return ResponseEntity.ok().body(dtos);
    }

}
