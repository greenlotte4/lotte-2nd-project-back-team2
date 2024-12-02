package com.backend.controller;

import com.backend.dto.request.calendar.*;
import com.backend.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CalendarController {

    private final CalendarService calendarService;

    @PostMapping("/calendar/content")
    public ResponseEntity<?> postCalendarContent (
            @RequestBody PostCalendarContentDto dto
            ) {
        ResponseEntity<?> response = calendarService.postCalendarContent(dto);
        return response;
    }

    @GetMapping("/calendar/content/name/today")
    public ResponseEntity<?> getCalendarContentToday () {
        ResponseEntity<?> response = calendarService.getCalendarContentToday();
        return response;
    }

    @GetMapping("/calendar/content/name/morning")
    public ResponseEntity<?> getCalendarContentMorning (
            @RequestParam String today
    ) {
        ResponseEntity<?> response = calendarService.getCalendarContentMorning(today);
        return response;
    }

    @GetMapping("/calendar/content/name/afternoon")
    public ResponseEntity<?> getCalendarContentAfternoon (
            @RequestParam String today
    ) {
        ResponseEntity<?> response = calendarService.getCalendarContentAfternoon(today);
        return response;
    }


    @GetMapping("/calendar/name")
    public ResponseEntity<?> getCalendarName (
            @RequestParam Long id
    ){
        ResponseEntity<?> response = calendarService.getCalendarName(id);
        return response;
    }

    @GetMapping("/calendar")
    public ResponseEntity<?> getCalendar (
            @RequestParam(value = "calendarId",defaultValue = "0") Long calendarId
    ){
        if(calendarId==0){
            ResponseEntity<?> response = calendarService.getCalendar();
            return response;
        } else {
            ResponseEntity<?> response = calendarService.getCalendarByCalendarId(calendarId);
            return response;
        }
    }

    @PutMapping("/calendar/contents")
    public ResponseEntity<?> putCalendarContents (
            @RequestBody List<PutCalendarContentsDto> dtos
    ){
        ResponseEntity<?> response = calendarService.putCalendarContents(dtos);
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
            @RequestBody PostCalendarDto dto
            ){

        ResponseEntity<?> response = calendarService.postCalendar(dto);
        return response;
    }

    @GetMapping("/calendar/users")
    public ResponseEntity<?> getCalendarUsers (
            @RequestParam Long id
    ){
        ResponseEntity<?> response = calendarService.getCalendarByGroup(id);
        return response;
    }

    @PutMapping("/calendar")
    public ResponseEntity<?> putCalendar (
            @RequestBody PutCalendarDto dtos
            ){
        ResponseEntity<?> response = calendarService.putCalendar(dtos);
        return response;
    }

    @DeleteMapping("/calendar")
    public ResponseEntity<?> deleteCalendar (
            @RequestParam Long id
    ) {
        ResponseEntity<?> response = calendarService.deleteCalendar(id);
        return response;
    }
}
