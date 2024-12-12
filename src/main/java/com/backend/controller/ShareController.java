package com.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/share")
public class ShareController {


    @PostMapping("/departments")
    public ResponseEntity shareDepartments(@RequestBody Map<String,  Map<String, Object>> selectedDepartments){

        log.info("부서 공유 들어옴" + selectedDepartments);
        return null;
    }
}
