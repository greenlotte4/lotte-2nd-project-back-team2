package com.backend.controller.admin;

import com.backend.dto.request.admin.group.PostAdminGroupDepartment;
import com.backend.dto.response.admin.group.GetGroupDto;
import com.backend.service.GroupService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminGroupController {

    private final GroupService groupService;

    @PostMapping("/group/department")
    public ResponseEntity<?> postAdminGroupDepartment (
        @RequestBody PostAdminGroupDepartment dto
    )
    {
        String company = "1246857";
        ResponseEntity<?> response = groupService.postAdminGroupDepartment(dto,company);
        return response;
    }

    @PostMapping("/group/team")
    public ResponseEntity<?> postAdminGroupTeam (
            @RequestBody PostAdminGroupDepartment dto
    )
    {
        String company = "1246857";
        ResponseEntity<?> response = groupService.postAdminGroupTeam(dto,company);
        return response;
    }

    @PutMapping("/group")
    public ResponseEntity<?> putAdminGroup(
            @RequestBody GetGroupDto dto
    ){
        ResponseEntity<?> response = groupService.putAdminGroup(dto);
        return response;
    }

    @GetMapping("/group")
    public ResponseEntity<?> getAdminGroup (
        @RequestParam String group,
        HttpServletRequest req
    ){
        String company = "1246857";
        ResponseEntity<?> response = groupService.getAdminGroup(group,company);
        return response;
    }

    @DeleteMapping("/group")
    public ResponseEntity<?> deleteAdminGroup(
            @RequestParam Long id,
            HttpServletRequest req
    ){
        ResponseEntity<?> response = groupService.deleteAdminGroup(id);

        return response;
    }

    @GetMapping("/group/users")
    public ResponseEntity<?> getAdminGroupUsers (
            HttpServletRequest req,
            @RequestParam String group
    ){
        String company = "1246857";
        ResponseEntity<?> response = groupService.getAdminGroupUsers(group,company);
        return response;
    }
}
