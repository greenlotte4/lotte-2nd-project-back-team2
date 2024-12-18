package com.backend.service;

import com.backend.document.calendar.CalendarAlert;
import com.backend.dto.request.calendar.*;
import com.backend.dto.response.calendar.GetCalendarContentNameDto;
import com.backend.dto.response.calendar.GetCalendarNameDto;
import com.backend.dto.response.calendar.GetCalendarsDto;
import com.backend.dto.response.user.GetUsersAllDto;
import com.backend.entity.calendar.Calendar;
import com.backend.entity.calendar.CalendarContent;
import com.backend.entity.calendar.CalendarMapper;
import com.backend.entity.user.User;
import com.backend.repository.calendar.CalendarAlertRepository;
import com.backend.repository.calendar.CalendarContentRepository;
import com.backend.repository.calendar.CalendarMapperRepository;
import com.backend.repository.calendar.CalendarRepository;
import com.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class CalendarService {
    private final CalendarRepository calendarRepository;
    private final CalendarContentRepository calendarContentRepository;
    private final UserRepository userRepository;
    private final CalendarMapperRepository calendarMapperRepository;
    private final CalendarAlertRepository calendarAlertRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ResponseEntity<?> getCalendarName(Long userId) {
        Map<String,Object> map = new HashMap<>();
        Optional<User> user = userRepository.findById(userId);
        if(user.isEmpty()){
            return ResponseEntity.ok().body("로그인 정보가 일치하지 않습니다.");
        }
        List<CalendarMapper> mappers = calendarMapperRepository.findAllByUserAndCalendar_StatusIsNot(user.get(),0);

        List<Calendar> calendars = mappers.stream().map(CalendarMapper::getCalendar).toList();

        if(calendars.isEmpty()){
            return ResponseEntity.ok("등록된 캘린더가 없습니다...");
        }
        List<GetCalendarNameDto> dtos = calendars.stream().map(Calendar::toGetCalendarNameDto).toList();
        return ResponseEntity.ok(dtos);
    }

    public ResponseEntity<?> getCalendar(Long id) {
        Optional<User> user = userRepository.findById(id);
        if(user.isEmpty()){
            return ResponseEntity.ok().body("로그인 정보가 일치하지 않습니다.");
        }
        List<CalendarMapper> mapper = calendarMapperRepository.findAllByUserAndCalendar_StatusIsNot(user.get(),0);
        if(mapper.isEmpty()){
            return ResponseEntity.ok("등록된 캘린더가 존재하지 않습니다.");
        }
        List<CalendarContent> calConts = new ArrayList<>();
        for (CalendarMapper calendarMapper : mapper) {
            List<CalendarContent> conts = calendarContentRepository.findAllByCalendarAndCalendar_StatusIsNotAndStatusIsNot(calendarMapper.getCalendar(),0,0);
            calConts.addAll(conts);
        }

        List<GetCalendarsDto> dtos = calConts.stream().map(CalendarContent::toGetCalendarsDto).toList();
        System.out.println(dtos);
        return ResponseEntity.ok(dtos);
    }

    public ResponseEntity<?> getCalendarByCalendarId(Long calendarId) {
        Optional<Calendar> calendar = calendarRepository.findByCalendarIdAndStatusIsNot(calendarId,0);
        if(calendar.isEmpty()){
            return ResponseEntity.ok("캘린더가 존재하지 않습니다.");
        }
        List<CalendarContent> contents = calendar.get().getCalendarContents();

        if(contents.isEmpty()){
            return ResponseEntity.ok("등록된 일정이 없습니다.");
        }
        List<CalendarContent> filteredContents = contents.stream().filter(v->{
            boolean isStatus = v.getStatus()!=0;
            return isStatus;
        }).toList();
        List<GetCalendarsDto> dtos = filteredContents.stream().map(CalendarContent::toGetCalendarsDto).toList();
        return ResponseEntity.ok(dtos);
    }

    public ResponseEntity<?> getCalendarContentToday(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if(user.isEmpty()){
            return ResponseEntity.badRequest().body("로그인 정보가 일치하지 않습니다...");
        }

        List<CalendarMapper> calendarMappers = calendarMapperRepository.findAllByUserAndCalendar_StatusIsNot(user.get(),0);
        if(calendarMappers.isEmpty()){
            return ResponseEntity.ok().body("등록된 캘린더가 없습니다.");
        }
        List<Calendar> calendars = calendarMappers.stream().map(CalendarMapper::getCalendar).toList();

        List<CalendarContent> contents = new ArrayList<>();
        for(Calendar calendar : calendars){
            List<CalendarContent> smallContents = calendar.getCalendarContents().stream()
                    .filter(v -> {
                        boolean isStatus = v.getStatus()!=0;
                        LocalDateTime startDate = v.getCalendarStartDate();
                        LocalDateTime endDate = v.getCalendarEndDate();
                        LocalDateTime now = LocalDateTime.now();
                        return (startDate.isEqual(now) || startDate.isBefore(now)) && (endDate.isEqual(now) || endDate.isAfter(now)) && isStatus;
                    })
                    .toList();
            contents.addAll(smallContents);
        }

        if(contents.isEmpty()){
            return ResponseEntity.ok("오늘 등록된 일정이 없습니다.");
        }

        List<GetCalendarContentNameDto> dtos = contents.stream().map(CalendarContent::toGetCalendarContentNameDto).toList();
        return ResponseEntity.ok(dtos);
    }

    public ResponseEntity<?> postCalendarContent(PostCalendarContentDto dto,Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if(user.isEmpty()){
            return ResponseEntity.badRequest().body("로그인 정보가 일치하지 않습니다...");
        }

        Optional<Calendar> calendar = calendarRepository.findByCalendarId(dto.getCalendarId());
        if(calendar.isEmpty()){
            return ResponseEntity.badRequest().body("등록된 캘린더가 없습니다.");
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        CalendarContent calendarContent = CalendarContent.builder()
                .calendar(calendar.get())
                .alertStatus(dto.getAlert())
                .calendarEndDate(LocalDateTime.parse(dto.getEdate(), formatter))
                .calendarStartDate(LocalDateTime.parse(dto.getSdate(), formatter))
                .name(dto.getTitle())
                .memo(dto.getMemo())
                .importance(dto.getImportance())
                .location(dto.getLocation())
                .status(1)
                .build();

        calendarContentRepository.save(calendarContent);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // String을 LocalDate로 변환
        LocalDate startDate = LocalDate.parse(dto.getSdate().substring(0,dto.getSdate().indexOf(" ")), dateFormatter);
        LocalDate endDate = LocalDate.parse(dto.getEdate().substring(0,dto.getEdate().indexOf(" ")), dateFormatter);
        List<CalendarMapper> calendarMapper = calendarMapperRepository.findAllByCalendar(calendar.get());
        List<Long> userIds2 = calendarMapper.stream().map(v->v.getUser().getId()).toList();
        List<CalendarAlert> alerts = new ArrayList<>();
        for (LocalDate date = startDate.plusDays(1); !date.isAfter(endDate.plusDays(1)); date = date.plusDays(1)) {
            CalendarAlert calendarAlert = CalendarAlert.builder()
                    .content(dto.getMemo())
                    .status(1)
                    .type(dto.getAlert())
                    .location(dto.getLocation())
                    .title(dto.getTitle())
                    .date(date)
                    .calendarId(calendar.get().getCalendarId())
                    .contentId(calendarContent.getCalendarContentId())
                    .time(dto.getSdate().substring(dto.getSdate().indexOf(" ")+1,dto.getSdate().length()-3))
                    .userIds(userIds2.toString())
                    .build();

            alerts.add(calendarAlert);
        }
        calendarAlertRepository.saveAll(alerts);

        Map<String, Object> map = new HashMap<>();
        map.put("message","등록이 완료되었습니다.");
        map.put("color",calendar.get().getColor());
        map.put("id",calendarContent.getCalendarContentId());
        return ResponseEntity.ok(map);
    }

    public ResponseEntity<?> putCalendarContents(PutCalendarContentsDto dto,Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if(user.isEmpty()){
            return ResponseEntity.badRequest().body("로그인 정보가 일치하지않습니다...");
        }

        Optional<CalendarContent> contents = calendarContentRepository.findByCalendarContentIdAndStatusIsNot(dto.getContentId(),0);
        if(contents.isEmpty()){
            return ResponseEntity.badRequest().body("수정할 캘린더가 존재하지 않습니다...");
        }

        Calendar calendar = contents.get().getCalendar();

        System.out.println("================sdfsdfsd==");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<CalendarAlert> alerts = calendarAlertRepository.findAllByContentId(dto.getContentId());
        calendarAlertRepository.deleteAll(alerts);

        LocalDate startDate = LocalDate.parse(dto.getStartDate().substring(0,dto.getStartDate().indexOf(" ")), dateFormatter);
        LocalDate endDate = LocalDate.parse(dto.getEndDate().substring(0,dto.getEndDate().indexOf(" ")), dateFormatter);
        List<CalendarMapper> calendarMapper = calendarMapperRepository.findAllByCalendar(calendar);
        List<Long> userIds2 = calendarMapper.stream().map(v->v.getUser().getId()).toList();
        List<CalendarAlert> newAlerts = new ArrayList<>();
        for (LocalDate date = startDate.plusDays(1); !date.isAfter(endDate.plusDays(1)); date = date.plusDays(1)) {
            CalendarAlert calendarAlert = CalendarAlert.builder()
                    .content(contents.get().getMemo())
                    .status(1)
                    .type(contents.get().getAlertStatus())
                    .location(contents.get().getLocation())
                    .title(dto.getTitle())
                    .date(date)
                    .calendarId(calendar.getCalendarId())
                    .contentId(dto.getContentId())
                    .time(dto.getStartDate().substring(dto.getStartDate().indexOf(" ")+1,dto.getStartDate().length()-3))
                    .userIds(userIds2.toString())
                    .build();

            newAlerts.add(calendarAlert);
        }
        calendarAlertRepository.saveAll(newAlerts);


        contents.get().patchContent(dto);
        return ResponseEntity.ok().body("수정이 완료되었습니다.");
    }

    public ResponseEntity<?> getCalendarContentAfternoon(String today,Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if(user.isEmpty()){
            return ResponseEntity.badRequest().body("로그인 정보가 일치하지않습니다...");
        }

        List<CalendarMapper> mappers = calendarMapperRepository.findAllByUserAndCalendar_StatusIsNot(user.get(),0);
        if(mappers.isEmpty()){
            return ResponseEntity.ok().body("등록된 캘린더가 없습니다.");
        }

        List<Calendar> calendars = mappers.stream().map(CalendarMapper::getCalendar).toList();

        List<CalendarContent> contents = new ArrayList<>();
        for (Calendar calendar : calendars) {
            String newToday = today + " 00:00:00";  // 특정 날짜의 기준을 00:00:00으로 설정
            List<CalendarContent> smallContents = calendar.getCalendarContents().stream()
                    .filter(v -> {
                        LocalDateTime startDate = v.getCalendarStartDate();  // 일정의 시작시간
                        LocalDateTime endDate = v.getCalendarEndDate();      // 일정의 종료시간
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        LocalDateTime now = LocalDateTime.parse(newToday, formatter);  // 특정 날짜 파싱

                        // 오늘의 12시부터 18시까지의 범위 설정
                        LocalDateTime todayStart = now.toLocalDate().atTime(12, 0);  // 오늘 12:00
                        LocalDateTime todayEnd = now.toLocalDate().atTime(18, 0);   // 오늘 18:00
                        boolean isStatus = v.getStatus()!=0;
                        boolean isTodayBetweenStartAndEnd = !now.toLocalDate().isBefore(startDate.toLocalDate()) &&
                                !now.toLocalDate().isAfter(endDate.toLocalDate());
                        // startDate와 endDate가 특정 날짜의 12시부터 18시 사이에 포함되는지 확인
                        boolean isStartTimeInRange = startDate.toLocalTime().isAfter(todayStart.toLocalTime()) &&
                                startDate.toLocalTime().isBefore(todayEnd.toLocalTime());  // startDate가 12시부터 18시 사이
                        boolean isEndTimeInRange = endDate.toLocalTime().isAfter(todayStart.toLocalTime()) &&
                                endDate.toLocalTime().isBefore(todayEnd.toLocalTime());    // endDate가 12시부터 18시 사이

                        // startDate와 endDate의 시간이 모두 12시~18시 사이에 포함되는지 체크
                        return isStartTimeInRange && isEndTimeInRange && isTodayBetweenStartAndEnd && isStatus;
                    })
                    .toList();

            contents.addAll(smallContents);  // 필터링된 결과를 contents에 추가
        }
        List<GetCalendarContentNameDto> dtos = contents.stream().map(CalendarContent::toGetCalendarContentNameDto).toList();
        return ResponseEntity.ok(dtos);
    }

    public ResponseEntity<?> getCalendarContentMorning(String today,Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if(user.isEmpty()){
            return ResponseEntity.badRequest().body("로그인 정보가 일치하지않습니다...");
        }

        List<CalendarMapper> mappers = calendarMapperRepository.findAllByUserAndCalendar_StatusIsNot(user.get(),0);
        if(mappers.isEmpty()){
            return ResponseEntity.ok().body("등록된 캘린더가 없습니다.");
        }

        List<Calendar> calendars = mappers.stream().map(CalendarMapper::getCalendar).toList();

        List<CalendarContent> contents = new ArrayList<>();
        for (Calendar calendar : calendars) {
            String newToday = today + " 00:00:00";  // 특정 날짜의 기준을 00:00:00으로 설정
            List<CalendarContent> smallContents = calendar.getCalendarContents().stream()
                    .filter(v -> {
                        LocalDateTime startDate = v.getCalendarStartDate();  // 일정의 시작시간
                        LocalDateTime endDate = v.getCalendarEndDate();      // 일정의 종료시간
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        LocalDateTime now = LocalDateTime.parse(newToday, formatter);  // 특정 날짜 파싱

                        // 오늘의 12시부터 18시까지의 범위 설정
                        LocalDateTime todayStart = now.toLocalDate().atTime(6, 0);  // 오늘 12:00
                        LocalDateTime todayEnd = now.toLocalDate().atTime(12, 0);   // 오늘 18:00
                        boolean isStatus = v.getStatus()!=0;
                        boolean isTodayBetweenStartAndEnd = !now.toLocalDate().isBefore(startDate.toLocalDate()) &&
                                !now.toLocalDate().isAfter(endDate.toLocalDate());
                        // startDate와 endDate가 특정 날짜의 12시부터 18시 사이에 포함되는지 확인
                        boolean isStartTimeInRange = startDate.toLocalTime().isAfter(todayStart.toLocalTime()) &&
                                startDate.toLocalTime().isBefore(todayEnd.toLocalTime());  // startDate가 12시부터 18시 사이
                        boolean isEndTimeInRange = endDate.toLocalTime().isAfter(todayStart.toLocalTime()) &&
                                endDate.toLocalTime().isBefore(todayEnd.toLocalTime());    // endDate가 12시부터 18시 사이

                        // startDate와 endDate의 시간이 모두 12시~18시 사이에 포함되는지 체크
                        return isStartTimeInRange && isEndTimeInRange && isTodayBetweenStartAndEnd && isStatus;
                    })
                    .toList();

            contents.addAll(smallContents);  // 필터링된 결과를 contents에 추가
        }
        List<GetCalendarContentNameDto> dtos = contents.stream().map(CalendarContent::toGetCalendarContentNameDto).toList();
        return ResponseEntity.ok(dtos);
    }

    public ResponseEntity<?> deleteCalendarContent(Long id) {
        Optional<CalendarContent> content = calendarContentRepository.findByCalendarContentId(id);
        if(content.isEmpty()){
            return ResponseEntity.badRequest().body("일정이 존재하지않습니다..");
        }

        List<CalendarAlert> alerts = calendarAlertRepository.findAllByContentId(content.get().getCalendarContentId());
        calendarAlertRepository.deleteAll(alerts);

        content.get().patchStatus(0);
        Map<String, Object> map = new HashMap<>();
        map.put("id",content.get().getCalendarContentId());
        map.put("message","삭제되었습니다.");
        return ResponseEntity.ok().body(map);
    }

    public ResponseEntity<?> putCalendarContent(PutCalendarContentDto dto) {
        Optional<CalendarContent> content = calendarContentRepository.findByCalendarContentId(dto.getCalendarId());
        if(content.isEmpty()){
            return ResponseEntity.badRequest().body("일정이 존재하지않습니다..");
        }
        Optional<Calendar> calendar = calendarRepository.findByCalendarId(dto.getSheave());
        if(calendar.isEmpty()){
            return ResponseEntity.badRequest().body("캘린더가 존재하지않습니다..");
        }
        System.out.println("================sdfsdfsd==");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<CalendarAlert> alerts = calendarAlertRepository.findAllByContentId(content.get().getCalendarContentId());
        calendarAlertRepository.deleteAll(alerts);

        LocalDate startDate = LocalDate.parse(dto.getSdate().substring(0,dto.getSdate().indexOf(" ")), dateFormatter);
        LocalDate endDate = LocalDate.parse(dto.getEdate().substring(0,dto.getEdate().indexOf(" ")), dateFormatter);
        List<CalendarMapper> calendarMapper = calendarMapperRepository.findAllByCalendar(calendar.get());
        List<Long> userIds2 = calendarMapper.stream().map(v->v.getUser().getId()).toList();
        List<CalendarAlert> newAlerts = new ArrayList<>();
        for (LocalDate date = startDate.plusDays(1); !date.isAfter(endDate.plusDays(1)); date = date.plusDays(1)) {
            CalendarAlert calendarAlert = CalendarAlert.builder()
                    .content(dto.getMemo())
                    .status(1)
                    .type(dto.getAlert())
                    .location(dto.getLocation())
                    .title(dto.getTitle())
                    .date(date)
                    .calendarId(calendar.get().getCalendarId())
                    .contentId(dto.getCalendarId())
                    .time(dto.getSdate().substring(dto.getSdate().indexOf(" ")+1,dto.getSdate().length()-3))
                    .userIds(userIds2.toString())
                    .build();

            newAlerts.add(calendarAlert);
        }
        calendarAlertRepository.saveAll(newAlerts);

        String color = calendar.get().getColor();
        content.get().putContent(dto,calendar.get());

        Map<String, Object> map = new HashMap<>();
        map.put("message","일정이 수정되었습니다.");
        map.put("color",color);
        return ResponseEntity.ok().body(map);
    }

    public ResponseEntity<?> postCalendar(PostCalendarDto dto, Long myId) {
        if(dto.getStatus()==1){
            Optional<User> user = userRepository.findById(myId);
            if(user.isEmpty()){
                return ResponseEntity.badRequest().body("로그인 정보가 일치하지않습니다...");
            }
            Optional<CalendarMapper> mapper = calendarMapperRepository.findByUserAndCalendar_Status(user.get(),1);
            mapper.ifPresent(calendarMapper -> calendarMapper.getCalendar().patchStatus(2));
        }
        Calendar calendar = Calendar.builder()
                .name(dto.getName())
                .color(dto.getColor())
                .status(dto.getStatus())
                .build();

        calendarRepository.save(calendar);

        List<Long> userIds = dto.getUserIds();
        userIds.add(myId);
        List<CalendarMapper> mappers = new ArrayList<>();
        for (Long userId : userIds) {
            Optional<User> user = userRepository.findById(userId);
            if(user.isEmpty()){
                return ResponseEntity.badRequest().body("공유멤버의 회원정보가 일치하지않습니다...");
            }
            CalendarMapper mapper = CalendarMapper.builder()
                    .user(user.get())
                    .calendar(calendar)
                    .build();
            mappers.add(mapper);
        }

        calendarMapperRepository.saveAll(mappers);

        GetCalendarNameDto nameDto = calendar.toGetCalendarNameDto();
        Map<String, Object> map = new HashMap<>();
        map.put("message","캘린더 등록이 완료되었습니다.");
        map.put("calendarName",nameDto);
        map.put("myid",myId);
        map.put("calendarId",calendar.getCalendarId());
        return ResponseEntity.ok(map);
    }

    public ResponseEntity<?> getCalendarByGroup(Long id,Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if(user.isEmpty()){
            return ResponseEntity.badRequest().body("로그인 정보가 일치하지않습니다.");
        }
        Optional<Calendar> calendar = calendarRepository.findByCalendarId(id);
        if(calendar.isEmpty()){
            return ResponseEntity.badRequest().body("캘린더 정보가 일치하지않습니다.");
        }
        List<CalendarMapper> mappers = calendarMapperRepository.findAllByCalendar(calendar.get());
        if(mappers.isEmpty()){
            return ResponseEntity.badRequest().body("캘린더 정보가 일치하지않습니다.");
        }
        List<GetUsersAllDto> dtos = new ArrayList<>();
        for (CalendarMapper mapper : mappers) {
            GetUsersAllDto mapperUser = mapper.getUser().toGetUsersAllDto();
            dtos.add(mapperUser);
        }

        return ResponseEntity.ok(dtos);
    }

    public ResponseEntity<?> putCalendar(PutCalendarDto dtos,Long myId) {
        if(dtos.getStatus()==1){
            Optional<User> user = userRepository.findById(myId);
            if(user.isEmpty()){
                return ResponseEntity.badRequest().body("로그인 정보가 일치하지않습니다...");
            }
            Optional<CalendarMapper> mapper = calendarMapperRepository.findByUserAndCalendar_Status(user.get(),1);
            mapper.ifPresent(calendarMapper -> calendarMapper.getCalendar().patchStatus(2));
        }

        Optional<Calendar> optCalendar = calendarRepository.findByCalendarId(dtos.getId());
        if(optCalendar.isEmpty()){
            return ResponseEntity.badRequest().body("캘린더 정보가 일치하지않습니다...");
        }

        optCalendar.get().putCalendar(dtos);

        List<CalendarMapper> mappers = calendarMapperRepository.findAllByCalendar(optCalendar.get());

        if(mappers.isEmpty()){
            return ResponseEntity.badRequest().body("캘린더에 공유인원이 없습니다...");
        }
        List<Long> ids = dtos.getUsers().stream().map(GetUsersAllDto::getId).toList();
        List<Long> originIds = mappers.stream().map(v->v.getUser().getId()).toList();

        List<Long> sortedIds = new ArrayList<>(ids);
        List<Long> sortedOriginIds = new ArrayList<>(originIds);

        Collections.sort(sortedIds);
        Collections.sort(sortedOriginIds);

        boolean areEqual = sortedIds.equals(sortedOriginIds);
        if(!areEqual){
            List<CalendarAlert> alerts = calendarAlertRepository.findAllByCalendarId(dtos.getId());
            List<CalendarAlert> newAlerts = new ArrayList<>();
            for (CalendarAlert alert : alerts) {
                alert.patchUsers(sortedIds.toString());
                newAlerts.add(alert);
            }
            calendarAlertRepository.saveAll(newAlerts);
        }

        calendarMapperRepository.deleteAll(mappers);


        for (Long id : ids) {
            Optional<User> user = userRepository.findById(id);
            if(user.isEmpty()){
                return ResponseEntity.badRequest().body("유저정보가 일치하지 않습니다.");
            }
            CalendarMapper mapper = CalendarMapper.builder()
                    .user(user.get())
                    .calendar(optCalendar.get())
                    .build();

            calendarMapperRepository.save(mapper);
        }

        return ResponseEntity.ok("캘린더 수정이 완료되었습니다.");
    }

    public ResponseEntity<?> deleteCalendar(Long id) {
        Optional<Calendar> calendar = calendarRepository.findById(id);
        if(calendar.isEmpty()){
            return ResponseEntity.badRequest().body("캘린더 정보가 일치하지않습니다...");
        }
        calendar.get().patchStatus(0);

        List<CalendarMapper> mappers = calendarMapperRepository.findAllByCalendar(calendar.get());
        if(mappers.isEmpty()){
            return ResponseEntity.ok("캘린더 공유인원이 없습니다...");
        }
        List<CalendarAlert> alerts = calendarAlertRepository.findAllByCalendarId(id);
        calendarAlertRepository.deleteAll(alerts);

        calendarMapperRepository.deleteAll(mappers);

        List<CalendarContent> contents = calendarContentRepository.findAllByCalendarAndCalendar_StatusIsNot(calendar.get(),0);
        calendarContentRepository.deleteAll(contents);

        return ResponseEntity.ok("캘린더 삭제가 완료되었습니다.");
    }

    public ResponseEntity<?> getGroupIds(Long id) {
        Optional<User> user = userRepository.findById(id);
        if(user.isEmpty()){
            return ResponseEntity.badRequest().body("로그인 정보가 일치하지않습니다.");
        }
        Long userId = user.get().getId();

        List<CalendarMapper> mappers = calendarMapperRepository.findAllByUserAndCalendar_StatusIsNot(user.get(),0);
        if(mappers.isEmpty()){
            return ResponseEntity.ok().body("등록된 캘린더가 없습니다...");
        }
        List<Long> calendarIds = mappers.stream().map(v->v.getCalendar().getCalendarId()).toList();
        Map<String,Object> map = new HashMap<>();
        map.put("calendarIds",calendarIds);
        map.put("userId",userId);
        return ResponseEntity.ok(map);
    }


    @Scheduled(fixedRate = 60000) // 1분마다 실행
    public void pushAlert() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        String HM = String.format("%02d:%02d", now.getHour(), now.getMinute());
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59, 999999999);
        Date start = Date.from(startOfDay.toInstant(ZoneOffset.UTC));
        Date end = Date.from(endOfDay.toInstant(ZoneOffset.UTC));
        List<CalendarAlert> alerts = calendarAlertRepository.findAllByDateBetweenAndTime(start, end,HM);
        System.out.println(alerts);
        messagingTemplate.convertAndSend("/topic/alert",alerts);
    }

    //2024/12/18 박연화 추가
    public ResponseEntity<?> getCalendarContentNext(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if(user.isEmpty()){
            return ResponseEntity.badRequest().body("로그인 정보가 일치하지 않습니다...");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfTomorrow = now.plusDays(1).toLocalDate().atStartOfDay();
        LocalDateTime endOfTomorrow = startOfTomorrow.plusDays(1).minusSeconds(1);
        LocalDateTime endOfNextWeek = now.plusWeeks(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).toLocalDate().atTime(23, 59, 59);

        List<CalendarContent> calendarContents = calendarMapperRepository
                .findAllContentsWithinDateRange(user.get(),startOfTomorrow, endOfNextWeek);
        if(calendarContents.isEmpty()){
            return ResponseEntity.ok().body(null);
        }
        log.info("내일부터 다음주까지의 캘린더 "+calendarContents.toString());
        List<CalendarContent> contents = calendarContents.stream().filter( v -> {
            boolean isStatus = v.getStatus()!=0;
            LocalDateTime startDate = v.getCalendarStartDate();
            LocalDateTime endDate = v.getCalendarEndDate();

            return startDate.isBefore(endOfTomorrow) && endDate.isAfter(startOfTomorrow) && isStatus;
        }).toList();

        log.info("필터링 한 거요 "+contents.toString());

        List<GetCalendarContentNameDto> dtos = new ArrayList<>();
        Map<String, List<GetCalendarContentNameDto>> map = new HashMap<>();
        if(contents.isEmpty()){
            dtos = calendarContents.stream().map(CalendarContent::toGetCalendarContentNameDto).toList();
            map.put("nextWeek",dtos);
        }else{
            log.info("여기 안 들어와?");
            dtos = contents.stream().map(CalendarContent::toGetCalendarContentNameDto).toList();
            map.put("tomorrow",dtos);
        }
        log.info("전송 데이터 "+ map.toString());
        return ResponseEntity.ok(map);
    }
}
