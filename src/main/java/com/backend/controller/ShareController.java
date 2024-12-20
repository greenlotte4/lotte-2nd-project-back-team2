package com.backend.controller;

import com.backend.dto.request.drive.RemoveDepartmentRequestDto;
import com.backend.dto.request.drive.ShareRequestDto;
import com.backend.service.ShareService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api")
public class ShareController {


    private final ShareService shareService;

    @PostMapping("/share/drive/{type}/{id}")
    public ResponseEntity<?> shareDepartments(@RequestBody ShareRequestDto request, @PathVariable String type, @PathVariable String id) {
        log.info("Request received with type: {}, id: {}, body: {}", type, id, request);

        boolean result = shareService.shareUser(request, type, id);
        if(result) {
            return  ResponseEntity.ok().body("Request received");
        }else{
            return  ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body("Request failed");

        }
    }




    @PostMapping("/drive/share/department")
    public ResponseEntity<?> shareDepartment(@RequestParam String id ,@RequestParam String type,@RequestBody ShareRequestDto departments ) {
        log.info("여기지금,"+type+"id"+id+"request"+departments);

        boolean result = shareService.sharedDepartment(departments,type,id);

        if (result) {
            return ResponseEntity.ok().body(departments);
        }else{
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(departments);
        }

    }

    @PostMapping("/drive/share/remove-department")
    public ResponseEntity<?> removeDepartment(@RequestBody RemoveDepartmentRequestDto request, HttpServletRequest servletRequest) {
        log.info("여기여기여기!!!!"+request);
        String remover = (String) servletRequest.getAttribute("uid");

        if(remover.equals(request.getOwnerId())) {

            shareService.deletedDepartment(request);

            return ResponseEntity.ok().body("삭제성공");

        }else{



        }
        return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(Collections.emptyList());
    }

    @GetMapping("/invite/validate/{invitationId}")
    public ResponseEntity<?> validateInvitation(@PathVariable String invitationId , HttpServletRequest servletRequest) {

        String uid = (String) servletRequest.getAttribute("uid");
        Map<String, Object> response = shareService.invitationInvaild(invitationId,uid);
        log.info(" 공유결과!!!!"+response);
        return ResponseEntity.ok().body(response);
    }
}
