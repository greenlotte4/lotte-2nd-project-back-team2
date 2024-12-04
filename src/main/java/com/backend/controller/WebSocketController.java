package com.backend.controller;

import com.backend.dto.request.calendar.PostCalendarDto;
import com.backend.dto.request.calendar.PutCalendarContentsDto;
import com.backend.dto.response.calendar.GetCalendarNameDto;
import com.backend.dto.response.calendar.GetCalendarsDto;
import com.backend.dto.response.calendar.GetMessagePostCalendarDto;
import com.backend.entity.calendar.Calendar;
import com.backend.entity.calendar.CalendarContent;
import com.backend.entity.calendar.CalendarMapper;
import com.backend.entity.user.User;
import com.backend.repository.UserRepository;
import com.backend.repository.calendar.CalendarContentRepository;
import com.backend.repository.calendar.CalendarMapperRepository;
import com.backend.repository.calendar.CalendarRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Log4j2
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    private final Map<String, List<String>> userCalendarSubscriptions = new HashMap<>(); // userId -> List<calendarId>
    private final UserRepository userRepository;
    private final CalendarMapperRepository calendarMapperRepository;
    private final CalendarContentRepository calendarContentRepository;
    private final CalendarRepository calendarRepository;

    @MessageMapping("/calendar/update")
    @Transactional
    public void updateCalendar(String calendarId) {
        List<CalendarContent> contents = calendarContentRepository.findAllByCalendar_CalendarIdAndCalendar_StatusIsNot(Long.parseLong(calendarId),0);
        Map<String, Object> map = new HashMap<>();
        List<GetCalendarsDto> dtos = contents.stream().map(CalendarContent::toGetCalendarsDto).toList();
        map.put("update", dtos);

        Optional<Calendar> calendar = calendarRepository.findByCalendarId(Long.parseLong(calendarId));
        if(calendar.isPresent()) {
            GetCalendarNameDto dto = calendar.get().toGetCalendarNameDto();
            map.put("name", dto);
        }

        messagingTemplate.convertAndSend( "/topic/calendar/"+calendarId, map);
    }

    @MessageMapping("/calendar/delete")
    @Transactional
    public void deleteCalendar(String calendarId) {
        List<CalendarContent> contents = calendarContentRepository.findAllByCalendar_CalendarIdAndCalendar_StatusIsNot(Long.parseLong(calendarId),0);
        Map<String, Object> map = new HashMap<>();
        List<GetCalendarsDto> dtos = contents.stream().map(CalendarContent::toGetCalendarsDto).toList();
        map.put("delete", calendarId);

        messagingTemplate.convertAndSend("/topic/calendar/"+calendarId, map);
    }

    @MessageMapping("/calendar/post")
    @Transactional
    public void postCalendar(@Payload String message) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        GetCalendarNameDto messageMap = objectMapper.readValue(message, GetCalendarNameDto.class);
        Map<String,Object> map = new HashMap<>();
        List<Long> userIds = messageMap.getUserIds();
        userIds.add(messageMap.getMyid());
        GetMessagePostCalendarDto dto = GetMessagePostCalendarDto.builder()
                .color(messageMap.getColor())
                .name(messageMap.getName())
                .status(messageMap.getStatus())
                .build();
        map.put("post",dto);
        for (Long userId : userIds) {
            messagingTemplate.convertAndSend("/topic/calendar/user/"+userId, map);
        }
    }

    @MessageMapping("/calendar/contents/put")
    @Transactional
    public void putCalendarContents(@Payload String message) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        PutCalendarContentsDto messageMap = objectMapper.readValue(message, PutCalendarContentsDto.class);
        Optional<Calendar> calendar = calendarRepository.findByCalendarContents_CalendarContentId(messageMap.getContentId());
        Long calendarId = calendar.get().getCalendarId();
        Map<String, Object> map = new HashMap<>();
        map.put("contentsPut",messageMap);
        messagingTemplate.convertAndSend("/topic/calendar/"+calendarId, map);
    }

    // 특정 사용자가 캘린더를 구독
    @MessageMapping("/subscribe")
    public void subscribeToCalendar(@Payload String jsonMessage) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> messageMap = objectMapper.readValue(jsonMessage, Map.class);

        String userId = messageMap.get("message");
        Optional<User> user = userRepository.findById(Long.parseLong(userId));
        if(user.isEmpty()){
            return;
        }
        List<CalendarMapper> mappers = calendarMapperRepository.findAllByUserAndCalendar_StatusIsNot(user.get(),0);
        List<String> calendarIds = new ArrayList<>();
        for (CalendarMapper mapper : mappers) {
            String id = String.valueOf(mapper.getCalendar().getCalendarId());
            calendarIds.add(id);
        }

        // 사용자가 구독할 캘린더 목록을 구독 처리
        userCalendarSubscriptions.put(userId, calendarIds);

    }

    // 특정 사용자가 캘린더 구독을 취소
    @MessageMapping("/unsubscribe")
    public void unsubscribeFromCalendar(String userId, String calendarId) {
        List<String> subscriptions = userCalendarSubscriptions.get(userId);
        if (subscriptions != null) {
            subscriptions.remove(calendarId);
        }
        System.out.println(userId + " has unsubscribed from calendar " + calendarId);
    }
}
