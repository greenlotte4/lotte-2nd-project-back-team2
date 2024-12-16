package com.backend.controller;

import com.backend.document.page.Page;
import com.backend.dto.request.page.PageDto;
import com.backend.dto.response.user.GetUsersAllDto;
import com.backend.entity.folder.Permission;
import com.backend.service.mongoDB.PageService;
import com.backend.util.PermissionType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Log4j2
@RequiredArgsConstructor
public class PageController {


    private final PageService pageService;


    @PostMapping("/page")
    public ResponseEntity<?> postNewPage(HttpServletRequest request){
        String uid = (String)request.getAttribute("uid");
        ResponseEntity<?> resp = pageService.postNewPage(uid);
        return resp;
    }


    @PostMapping("/page/save")
    public ResponseEntity<?> save(HttpServletRequest request, @RequestBody PageDto pageDto) {
        String uid = (String)request.getAttribute("uid");
        log.info("세이브되나?"+pageDto);

        Page page = pageService.save(pageDto);
        return ResponseEntity.ok().body(page);
    }

    @GetMapping("/page/view/{id}")
    public ResponseEntity<?> view (@PathVariable String id){

        log.info("요청이 들어오나?? /view"+id);
        Map<String,Object> map = new HashMap<>();
        PageDto pageDto = pageService.findById(id);

        map.put("pageDto",pageDto);
        log.info("나온값!!1"+pageDto);

        return ResponseEntity.ok(pageDto);
    }

    @GetMapping("/page/list")
    public ResponseEntity<?> list(HttpServletRequest request){
        String uid = (String)request.getAttribute("uid");

        List<Page> pages = pageService.pageList(uid);

        return ResponseEntity.ok().body(pages);

    }

    @GetMapping("/page/content/{pageId}")
    public ResponseEntity<?> getPageContent(
            HttpServletRequest req,
            @PathVariable String pageId
    ){
        System.out.println(pageId);
        Long userId = (Long) req.getAttribute("id");
        ResponseEntity<?> response = pageService.getPageContent(pageId, userId);
        return response;
    }

    @PutMapping("/page/content/{pageId}")
    public ResponseEntity<?> putPageContent(
            HttpServletRequest req,
            @PathVariable String pageId,
            @RequestBody Object content
    ) throws JsonProcessingException {
        System.out.println("이거되고있냐????");
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode contentNode = objectMapper.valueToTree(content); // content를 JsonNode로 변환

        // "content" 필드를 제거
        JsonNode contentData = contentNode.get("content");

        // contentData를 다시 JSON 문자열로 변환
        String contentString = objectMapper.writeValueAsString(contentData);

        // 확인
        System.out.println(contentString);
        System.out.println(pageId);
        Long userId = (Long)req.getAttribute("id");
        PageDto pageDto = PageDto.builder().content(contentString).id(pageId).build();
        ResponseEntity<?> response = pageService.putPageContent(pageDto,userId);
        return response;
    }

    @GetMapping("/page/users/{pageId}")
    public ResponseEntity<?> getPageUsers(
            @PathVariable String pageId
    ){
        ResponseEntity<?> resp = pageService.getPageUsers(pageId);
        return resp;
    }

    @GetMapping("/page/title/{pageId}")
    public ResponseEntity<?> getPageTitle (
        @PathVariable String pageId
    ){
        ResponseEntity<?> resp = pageService.getPageTitle(pageId);
        return resp;
    }

    @PutMapping("/page/title")
    public ResponseEntity<?> putPageTitle (
            @RequestParam String pageId,
            @RequestParam String title
    ){
        ResponseEntity<?> resp = pageService.putPageTitle(pageId,title);
        return resp;
    }

    @PutMapping("/page/users")
    public ResponseEntity<?> putPageUsers(
            @RequestParam String pageId,
            @RequestBody List<GetUsersAllDto> users
    ){
        ResponseEntity<?> resp = pageService.putPageUsers(pageId,users);
        return resp;
    }

    @DeleteMapping("/page/{pageId}")
    public ResponseEntity<?> deletePage(@PathVariable String pageId){
        ResponseEntity<?> resp = pageService.deletePage(pageId);
        return resp;
    }

}
