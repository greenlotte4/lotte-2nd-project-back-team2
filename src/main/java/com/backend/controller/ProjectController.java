package com.backend.controller;

import com.backend.dto.request.project.PatchCoworkersDTO;
import com.backend.dto.request.project.PostProjectDTO;
import com.backend.dto.response.project.GetProjectColumnDTO;
import com.backend.dto.response.project.GetProjectDTO;
import com.backend.dto.response.project.GetProjectSubTaskDTO;
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
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Log4j2
@RequestMapping("/api")
@RequiredArgsConstructor
@RestController
public class ProjectController {

    private final SimpMessagingTemplate messagingTemplate;
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



    @MessageMapping("/project/{id}/column/post") // 프로젝트 컬럼 생성
    public void createColumn(@DestinationVariable Long id, @Payload GetProjectColumnDTO dto) {
        ProjectColumn column = projectService.addColumn(dto, id);
        messagingTemplate.convertAndSend("/topic/project/"+id+"/column",column.toGetProjectColumnDTO());
    }
    @MessageMapping("/project/{id}/task/post")
    public void upsertTask(@DestinationVariable Long id, @Payload GetProjectTaskDTO dto) { // 태스크 생성, 수정
        log.info("GetProjectTaskDTO: {}",dto);
        ProjectTask task = projectService.saveTask(dto);
        messagingTemplate.convertAndSend("/topic/project/"+id+"/task",task.toGetProjectTaskDTO());
    }
    @MessageMapping("/project/coworkers/update") // 프로젝트 작업자 목록 수정
    public void updateCoworkers(@Payload PatchCoworkersDTO dto) {
        projectService.updateCoworkers(dto);
        messagingTemplate.convertAndSend("/topic/project/"+dto.getProjectId()+"/coworkers", dto);
    }
    @MessageMapping("/project/sub/post")
    public ResponseEntity<?> createSubTask(@Payload GetProjectSubTaskDTO dto){
        GetProjectSubTaskDTO saved = projectService.insertSubTask(dto);
        return ResponseEntity.ok().body(saved);
    }
    @MessageMapping("/project/sub/{id}/click")
    public ResponseEntity<?> updateSubTask(@DestinationVariable long id){
        boolean result = projectService.clickSubTask(id);
        log.info("SubTask(ID "+ id +") is Checked : " + result);
        if(result){
            return ResponseEntity.ok().build();
        }else{
            return ResponseEntity.status(500).build();
        }
    }

    @MessageMapping("/project/delete/{type}/{id}")
    public ResponseEntity<?> deleteProject(@DestinationVariable String type, @DestinationVariable Long id) {
        log.info("delete "+type+" id : "+id);
        projectService.delete(type, id);
        return ResponseEntity.ok().build();
    }

}
