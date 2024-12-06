package com.backend.controller.admin;

import com.backend.dto.response.admin.user.GetGroupUsersDto;
import com.backend.dto.response.user.GetUsersAllDto;
import com.backend.service.GroupService;
import com.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminUserController {

    private final GroupService groupService;
    private final UserService userService;

    @GetMapping("/users/all")
    public ResponseEntity<?> getAllUser2(
            @RequestParam int page,
            @RequestParam (value = "keyword", defaultValue = "") String keyword,
            @RequestParam(value = "id", defaultValue = "0") Long id
    ){
        Map<String, Object> map = new HashMap<>();
        Page<GetGroupUsersDto> dtos;
        if(!keyword.equals("")&&id==0){
            dtos = userService.getAdminUsersAllByKeyword(page,keyword);
        } else if (!keyword.equals("")&&id!=0) {
            dtos = userService.getAdminUsersAllByKeywordAndGroup(page,keyword,id);
        } else if (keyword.equals("")&& id!=0) {
            dtos = userService.getAdminUsersAllByGroup(page,id);
        } else {
            dtos = userService.getAdminUsersAll(page);
        }

        map.put("users", dtos.getContent());
        map.put("totalPages", dtos.getTotalPages());
        map.put("totalElements", dtos.getTotalElements());
        map.put("currentPage", dtos.getNumber());
        map.put("hasNextPage", dtos.hasNext());

        return ResponseEntity.ok().body(map);
    }
}
