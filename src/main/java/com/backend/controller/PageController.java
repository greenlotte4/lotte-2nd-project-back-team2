package com.backend.controller;

import com.backend.document.page.Page;
import com.backend.dto.request.page.PageDto;
import com.backend.entity.folder.Permission;
import com.backend.service.mongoDB.PageService;
import com.backend.util.PermissionType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/page")
@Log4j2
@RequiredArgsConstructor
public class PageController {


    private final PageService pageService;

    @GetMapping("/newPage")
    public ResponseEntity newPage(HttpServletRequest request) {

        String uid = (String)request.getAttribute("uid");
        PageDto pageDto = PageDto.builder()
                .title("제목 없음")
                .content(null)
                .createAt(LocalDateTime.now())
                .permissions(PermissionType.FULL.name())
                .ownerUid(uid)
                .build();

        pageDto.setOwnerUid(uid);

        Map<String,Object> response = new HashMap<>();

        Page page = pageService.save(pageDto);
        response.put("id",page.getId());

      return ResponseEntity.ok().body(response);
    }


    @PostMapping("/save")
    public ResponseEntity save(HttpServletRequest request, @RequestBody PageDto pageDto) {
        String uid = (String)request.getAttribute("uid");
        log.info("세이브되나?"+pageDto);

        Page page = pageService.save(pageDto);
        return ResponseEntity.ok().body(page);
    }

    @GetMapping("/view/{id}")
    public ResponseEntity view (@PathVariable String id){

        log.info("요청이 들어오나?? /view"+id);
        Map<String,Object> map = new HashMap<>();
        PageDto pageDto = pageService.findById(id);

        map.put("pageDto",pageDto);
        log.info("나온값!!1"+pageDto);

        return ResponseEntity.ok(pageDto);
    }
}
