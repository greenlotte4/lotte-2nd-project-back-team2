package com.backend.controller;

import com.backend.dto.request.project.PostProjectDTO;
import com.backend.dto.response.project.GetProjectDTO;
import com.backend.entity.project.Project;
import com.backend.service.ProjectService;
import com.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Log4j2
@RequestMapping("/api")
@RequiredArgsConstructor
@RestController
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping("/project")
    public ResponseEntity<?> createProject(@RequestBody PostProjectDTO dto, HttpServletRequest request) {

        String ownerUid = (String) request.getAttribute("uid");
        Project savedProject = projectService.createProject(dto, ownerUid);

        return ResponseEntity.ok().body(savedProject);
    }

    @GetMapping("/projects")
    public ResponseEntity<?> readProjectList(HttpServletRequest request) {

        String ownerUid = (String) request.getAttribute("uid");
        Map<String,Object> map = projectService.getAllProjects(ownerUid);

        return ResponseEntity.ok().body(map);
    }
    @GetMapping("/project/{id}")
    public ResponseEntity<?> readProject( @PathVariable Long id ) {

        GetProjectDTO dto = projectService.getProject(id);

        return ResponseEntity.ok().body(dto);
    }

}
