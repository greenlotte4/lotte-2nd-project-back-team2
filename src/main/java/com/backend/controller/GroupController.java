package com.backend.controller;

import com.backend.dto.request.PostDepartmentReqDto;
import com.backend.dto.response.GetAdminUsersRespDto;
import com.backend.dto.response.group.GetGroupsAllDto;
import com.backend.dto.response.user.GetUsersAllDto;
import com.backend.service.GroupService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.coyote.Response;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GroupController {
    private final GroupService groupService;

    @GetMapping("/departments")
    public ResponseEntity<?> getDepartments() {
        ResponseEntity<?> response = groupService.getDepartments();
        return response;
    }

    @GetMapping("/teams")
    public ResponseEntity<?> getTeams() {
        ResponseEntity<?> response = groupService.getTeams();
        return response;
    }

    @PostMapping("/department")
    public ResponseEntity<?> postDepartment(
            @RequestBody PostDepartmentReqDto dto
            ) {
        ResponseEntity<?> response = groupService.postDepartment(dto);
        return response;
    }

    @PostMapping("/team")
    public ResponseEntity<?> postTeam(
            @RequestBody PostDepartmentReqDto dto
    ) {
        ResponseEntity<?> response = groupService.postTeam(dto);
        return response;
    }

    @GetMapping("/group/leader")
    public ResponseEntity<?> getLeader(
            @RequestParam(value = "team",defaultValue = "") String team
    ) {
        ResponseEntity<?> response = groupService.getLeader(team);
        return response;
    }

    @PatchMapping("/group/leader")
    public ResponseEntity<?> patchLeader(
            @RequestParam Long id,
            @RequestParam String name
            ){
        ResponseEntity<?> response = groupService.patchLeader(id,name);
        return response;
    }

    @GetMapping("/group/users")
    public ResponseEntity<?> getUsers(
            @RequestParam(value = "team",defaultValue = "") String team
    ){
        log.info("team : " + team);
        ResponseEntity<?> response = groupService.getGroupMembers(team);
        return response;
    }

    @GetMapping("/group/users/detail")
    public ResponseEntity<?> getUsersDtail(
            @RequestParam(value = "team",defaultValue = "") String team,
            @RequestParam(value = "condition",defaultValue = "0") String condition,
            @RequestParam(value = "keyword",defaultValue = "0") String keyword
    ){
        ResponseEntity<?> response;
        if(!condition.equals("0")&&!keyword.equals("0")){
            response = groupService.getGroupMembersDetailBySearch(team,condition,keyword);
        } else {
            response = groupService.getGroupMembersDetail(team);
        }

        return response;
    }

    @GetMapping("/group/users/approval")
    public ResponseEntity<?> getUsersApproval(){
        ResponseEntity<?> response = groupService.getGroupMembersApproval();
        return response;
    }

    @PatchMapping("/group/users")
    public ResponseEntity<?> patchUsers(
            @RequestParam List<Long> ids,
            @RequestParam String team
            ){
        System.out.println(ids);
        ResponseEntity<?> response = groupService.patchGroupMembers(ids,team);
        return response;
    }

    @PatchMapping("/group")
    public ResponseEntity<?> patchGroup(
            @RequestParam String name,
            @RequestParam String update
    ){
        ResponseEntity<?> response = groupService.patchGroupName(name,update);
        return response;
    }

    @GetMapping("/groups/all")
    public ResponseEntity<?> getAllGroups(
            @RequestParam int page,
            @RequestParam(value = "keyword",defaultValue = "") String keyword,
            @RequestParam(value = "id", defaultValue = "0") Long id,
            HttpServletRequest request
    ) {
        Map<String, Object> map = new HashMap<>();
        Page<GetGroupsAllDto> dtos;
        String company = (String) request.getAttribute("company") == null ? "": (String) request.getAttribute("company") ;
        if(!keyword.equals("")&&id==0){
            dtos = groupService.getGroupsAllByKeyword(page,keyword,company);
        } else {
            dtos = groupService.getGroupsAll(page,company);
        }

        map.put("groups", dtos.getContent());
        map.put("totalPages", dtos.getTotalPages());
        map.put("totalElements", dtos.getTotalElements());
        map.put("currentPage", dtos.getNumber());
        map.put("hasNextPage", dtos.hasNext());

        return ResponseEntity.ok().body(map);
    }

    @GetMapping("/groups/size/{groupId}")
    public ResponseEntity<?> groupSize(@PathVariable Long groupId){

        long size = groupService.getGroupSize(groupId);

        return  ResponseEntity.ok().body(size);
    }
}
