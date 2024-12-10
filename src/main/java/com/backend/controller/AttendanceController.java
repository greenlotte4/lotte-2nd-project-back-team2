package com.backend.controller;

import com.backend.dto.request.user.RequestVacationDTO;
import com.backend.service.AttendanceService;
import com.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Log4j2
public class AttendanceController {
    private final AttendanceService attendanceService;
    private final UserService userService;

    @GetMapping("/today")
    public ResponseEntity<?> getTodayAttendance(Authentication auth) {
        String uid = auth.getName();
        log.info("오늘 근태 컨트롤러 "+uid);
        Map<String, String> times = attendanceService.getTodayAttendance(uid);
        return ResponseEntity.ok().body(times);
    }

    @PostMapping("/checkIn")
    public ResponseEntity<?> checkIn(Authentication auth){
        String uid = auth.getName();
        ResponseEntity<?> result = attendanceService.goToWork(uid);
        return result;
    }

    @PostMapping("/checkOut")
    public ResponseEntity<?> checkOut(Authentication auth){
        String uid = auth.getName();
        return attendanceService.leaveWork(uid);
    }

    @PostMapping("/reqVacation")
    public ResponseEntity<?> requestVacation(Authentication auth,@RequestBody RequestVacationDTO reqVacationDTO){
        String uid = auth.getName();
        LocalDateTime reqDate = LocalDateTime.now();
        reqVacationDTO.setUserId(uid);
        reqVacationDTO.setRequestDate(reqDate);
        Boolean result = attendanceService.insertVacation(reqVacationDTO);
        return ResponseEntity.ok(result);
    }

}
