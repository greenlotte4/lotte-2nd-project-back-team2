package com.backend.controller;

import com.backend.dto.request.calendar.*;
import com.backend.service.CalendarService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CalendarController {

    private final CalendarService calendarService;

    @PostMapping("/calendar/content")
    public ResponseEntity<?> postCalendarContent (
            @RequestBody PostCalendarContentDto dto,
            HttpServletRequest req
    ) {
        Object idObj = req.getAttribute("id");
        Long id;
        if (idObj != null) {
            id = Long.valueOf(idObj.toString());  // 문자열을 Long으로 변환
        } else {
            id= 0L;
        }
        ResponseEntity<?> response = calendarService.postCalendarContent(dto,id);
        return response;
    }

    @GetMapping("/calendar/content/name/today")
    public ResponseEntity<?> getCalendarContentToday (HttpServletRequest req) {
        Object idObj = req.getAttribute("id");
        Long id;
        if (idObj != null) {
            id = Long.valueOf(idObj.toString());  // 문자열을 Long으로 변환
        } else {
            id= 0L;
        }
        ResponseEntity<?> response = calendarService.getCalendarContentToday(id);
        return response;
    }

    @GetMapping("/calendar/content/name/morning")
    public ResponseEntity<?> getCalendarContentMorning (
            @RequestParam String today,
            HttpServletRequest req
    ) {
        Object idObj = req.getAttribute("id");
        Long id;
        if (idObj != null) {
            id = Long.valueOf(idObj.toString());  // 문자열을 Long으로 변환
        } else {
            id= 0L;
        }
        ResponseEntity<?> response = calendarService.getCalendarContentMorning(today,id);
        return response;
    }

    @GetMapping("/calendar/content/name/afternoon")
    public ResponseEntity<?> getCalendarContentAfternoon (
            @RequestParam String today,
            HttpServletRequest req
    ) {
        Object idObj = req.getAttribute("id");
        Long id;
        if (idObj != null) {
            id = Long.valueOf(idObj.toString());  // 문자열을 Long으로 변환
        } else {
            id= 0L;
        }
        ResponseEntity<?> response = calendarService.getCalendarContentAfternoon(today,id);
        return response;
    }


    @GetMapping("/calendar/name")
    public ResponseEntity<?> getCalendarName (
            HttpServletRequest req
    ){
        Object idObj = req.getAttribute("id");
        Long id;
        if (idObj != null) {
            id = Long.valueOf(idObj.toString());  // 문자열을 Long으로 변환
        } else {
            id= 0L;
        }
        ResponseEntity<?> response = calendarService.getCalendarName(id);
        return response;
    }

    @GetMapping("/calendar")
    public ResponseEntity<?> getCalendar (
            @RequestParam(value = "calendarId",defaultValue = "0") Long calendarId,
            HttpServletRequest req
    ){
        Object idObj = req.getAttribute("id");
        Long id;
        if (idObj != null) {
            id = Long.valueOf(idObj.toString());  // 문자열을 Long으로 변환
        } else {
            id= 0L;
        }
        ResponseEntity<?> response = calendarService.getCalendar(id);
        return response;

    }

    @PutMapping("/calendar/contents")
    public ResponseEntity<?> putCalendarContents (
            @RequestBody PutCalendarContentsDto dto,
            HttpServletRequest req
    ){
        Object idObj = req.getAttribute("id");
        Long id;
        if (idObj != null) {
            id = Long.valueOf(idObj.toString());
        } else {
            id= 0L;
        }
        ResponseEntity<?> response = calendarService.putCalendarContents(dto,id);
        return response;
    }

    @PutMapping("/calendar/content")
    public ResponseEntity<?> putCalendarContents (
            @RequestBody PutCalendarContentDto dto
    ){
        ResponseEntity<?> response = calendarService.putCalendarContent(dto);
        return response;
    }

    @DeleteMapping("/calendar/content")
    public ResponseEntity<?> deleteCalendarContent (
            @RequestParam Long id
    ){
        ResponseEntity<?> response = calendarService.deleteCalendarContent(id);
        return response;
    }

    @PostMapping("/calendar")
    public ResponseEntity<?> postCalendar (
            @RequestBody PostCalendarDto dto,
            HttpServletRequest req
    ){
        Object idObj = req.getAttribute("id");
        Long id;
        if (idObj != null) {
            id = Long.valueOf(idObj.toString());  // 문자열을 Long으로 변환
        } else {
            id= 0L;
        }
        ResponseEntity<?> response = calendarService.postCalendar(dto,id);
        return response;
    }

    @GetMapping("/calendar/users")
    public ResponseEntity<?> getCalendarUsers (
            @RequestParam Long id,
            HttpServletRequest req
    ){
        Object idObj = req.getAttribute("id");
        Long userId;
        if (idObj != null) {
            userId = Long.valueOf(idObj.toString());  // 문자열을 Long으로 변환
        } else {
            userId= 0L;
        }
        ResponseEntity<?> response = calendarService.getCalendarByGroup(id,userId);
        return response;
    }

    @PutMapping("/calendar")
    public ResponseEntity<?> putCalendar (
            @RequestBody PutCalendarDto dtos,
            HttpServletRequest req
    ){
        Object idObj = req.getAttribute("id");
        Long userId;
        if (idObj != null) {
            userId = Long.valueOf(idObj.toString());  // 문자열을 Long으로 변환
        } else {
            userId= 0L;
        }
        ResponseEntity<?> response = calendarService.putCalendar(dtos,userId);
        return response;
    }

    @DeleteMapping("/calendar")
    public ResponseEntity<?> deleteCalendar (
            @RequestParam Long id
    ) {
        ResponseEntity<?> response = calendarService.deleteCalendar(id);
        return response;
    }

    @GetMapping("/calendar/groups")
    public ResponseEntity<?> getCalendarGroups (
            HttpServletRequest req
    ){
        Object idObj = req.getAttribute("id");
        Long id;
        if (idObj != null) {
            id = Long.valueOf(idObj.toString());  // 문자열을 Long으로 변환
        } else {
            id= 0L;
        }
        ResponseEntity<?> response = calendarService.getGroupIds(id);
        return response;
    }
    @GetMapping("/calendar/content/name/next")
    public ResponseEntity<?> getCalendarContentNext (HttpServletRequest req) {
        Object idObj = req.getAttribute("id");
        Long id;
        if (idObj != null) {
            id = Long.valueOf(idObj.toString());  // 문자열을 Long으로 변환
        } else {
            id= 0L;
        }
        ResponseEntity<?> response = calendarService.getCalendarContentNext(id);
        return response;
    }


}
