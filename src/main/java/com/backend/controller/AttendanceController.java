package com.backend.controller;

import com.backend.dto.request.user.ReqAttendanceDTO;
import com.backend.dto.request.user.RequestVacationDTO;
import com.backend.service.AttendanceService;
import com.backend.service.UserService;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
        Long uid = Long.parseLong(auth.getName());
        log.info("오늘 근태 컨트롤러 "+uid);
        Map<String, String> times = attendanceService.getTodayAttendance(uid);
        return ResponseEntity.ok().body(times);
    }

    @GetMapping("/week")
    public ResponseEntity<?> getWeekAttendance(
            Authentication auth,
            @RequestParam(name = "type",defaultValue = "0") int type
    ) {
        Long uid = Long.parseLong(auth.getName());
        return attendanceService.getWeekAttendance(uid, type);
    }

    @PostMapping("/searchDate")
    public ResponseEntity<?> searchAttendanceByDate(Authentication auth, @RequestBody ReqAttendanceDTO dto) {
        Long uid = Long.parseLong(auth.getName());
        log.info("근태 기간별 검색 컨트롤러 "+uid+dto);
        return attendanceService.searchByDate(uid, dto);
    }

    @PostMapping("/checkIn")
    public ResponseEntity<?> checkIn(Authentication auth){
        Long uid = Long.parseLong(auth.getName());
        log.info("겟네임 "+uid);
        ResponseEntity<?> result = attendanceService.goToWork(uid);
        return result;
    }

    @PostMapping("/checkOut")
    public ResponseEntity<?> checkOut(Authentication auth){
        Long uid = Long.parseLong(auth.getName());
        log.info("겟네임 "+uid);
        return attendanceService.leaveWork(uid);
    }

    @PostMapping("/reqVacation")
    public ResponseEntity<?> requestVacation(Authentication auth,@RequestBody RequestVacationDTO reqVacationDTO){
        Long uid = Long.parseLong(auth.getName());
        LocalDateTime reqDate = LocalDateTime.now();
        reqVacationDTO.setUserId(uid);
        reqVacationDTO.setRequestDate(reqDate);
        reqVacationDTO.setStatus(0);
        Boolean result = attendanceService.insertVacation(reqVacationDTO);
        return ResponseEntity.ok(result);
    }

}
