package com.backend.controller;

import com.backend.dto.request.calendar.PostCalendarDto;
import com.backend.dto.request.project.PostProjectDTO;
import com.backend.dto.response.user.GetUsersAllDto;
import com.backend.entity.project.Project;
import com.backend.entity.user.User;
import com.backend.service.ProjectService;
import com.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Log4j2
@RequestMapping("/api")
@RequiredArgsConstructor
@RestController
public class ProjectController {

    private final UserService userService;
    private final ProjectService projectService;

    @PostMapping("/project")
    public ResponseEntity<?> createProject(@RequestBody PostProjectDTO dto, HttpServletRequest request ) {

        User projectOwner = userService.getUserByuid((String) request.getAttribute("uid"));
        Project savedProject = projectService.createProject(dto, projectOwner);

        return ResponseEntity.ok().body(savedProject);
    }

}
