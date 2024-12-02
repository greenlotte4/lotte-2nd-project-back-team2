package com.backend.service;

import com.backend.dto.request.calendar.*;
import com.backend.dto.response.calendar.GetCalendarContentNameDto;
import com.backend.dto.response.calendar.GetCalendarNameDto;
import com.backend.dto.response.calendar.GetCalendarsDto;
import com.backend.dto.response.user.GetUsersAllDto;
import com.backend.entity.calendar.Calendar;
import com.backend.entity.calendar.CalendarContent;
import com.backend.entity.calendar.CalendarMapper;
import com.backend.entity.user.User;
import com.backend.repository.calendar.CalendarContentRepository;
import com.backend.repository.calendar.CalendarMapperRepository;
import com.backend.repository.calendar.CalendarRepository;
import com.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CalendarService {
    private final CalendarRepository calendarRepository;
    private final CalendarContentRepository calendarContentRepository;
    private final UserRepository userRepository;
    private final CalendarMapperRepository calendarMapperRepository;

    public ResponseEntity<?> getCalendarName(Long id) {
        Optional<User> user = userRepository.findById(9L);
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

    public ResponseEntity<?> getCalendar() {
        Optional<User> user = userRepository.findById(9L);
        if(user.isEmpty()){
            return ResponseEntity.ok().body("로그인 정보가 일치하지 않습니다.");
        }
        Optional<CalendarMapper> mapper = calendarMapperRepository.findByUserAndCalendar_Status(user.get(),1);
        if(mapper.isEmpty()){
            return ResponseEntity.ok("등록된 캘린더가 존재하지 않습니다.");
        }

        Calendar calendar = mapper.get().getCalendar();

        List<CalendarContent> contents = calendar.getCalendarContents();
        if(contents.isEmpty()){
            return ResponseEntity.ok("일정이 없습니다.");
        }
        List<CalendarContent> filteredContents = contents.stream().filter(v->{
            boolean isStatus = v.getStatus()!=0;
            return isStatus;
        }).toList();
        List<GetCalendarsDto> dtos = filteredContents.stream().map(CalendarContent::toGetCalendarsDto).toList();
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

    public ResponseEntity<?> getCalendarContentToday() {
        Long userId = 9L;
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

    public ResponseEntity<?> postCalendarContent(PostCalendarContentDto dto) {
        Long userId = 9L;
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
        Map<String, Object> map = new HashMap<>();
        map.put("message","등록이 완료되었습니다.");
        map.put("color",calendar.get().getColor());
        map.put("id",calendarContent.getCalendarContentId());
        return ResponseEntity.ok(map);
    }

    public ResponseEntity<?> putCalendarContents(List<PutCalendarContentsDto> dtos) {
        Long userId = 9L;
        Optional<User> user = userRepository.findById(userId);
        if(user.isEmpty()){
            return ResponseEntity.badRequest().body("로그인 정보가 일치하지않습니다...");
        }
        for(PutCalendarContentsDto dto : dtos){
            Optional<CalendarContent> content = calendarContentRepository.findByCalendarContentId(dto.getContentId());
            if(content.isEmpty()){
                return ResponseEntity.badRequest().body("일정 정보가 일치하지않습니다...");
            }
            content.get().putContents(dto);
        }

        return ResponseEntity.ok().body("수정이 완료되었습니다.");
    }

    public ResponseEntity<?> getCalendarContentAfternoon(String today) {
        Long userId = 9L;
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

    public ResponseEntity<?> getCalendarContentMorning(String today) {
        Long userId = 9L;
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
        String color = calendar.get().getColor();
        content.get().putContent(dto,calendar.get());

        Map<String, Object> map = new HashMap<>();
        map.put("message","일정이 수정되었습니다.");
        map.put("color",color);
        return ResponseEntity.ok().body(map);
    }

    public ResponseEntity<?> postCalendar(PostCalendarDto dto) {
        Long myId = 9L;
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
        return ResponseEntity.ok(map);
    }

    public ResponseEntity<?> getCalendarByGroup(Long id) {
        Long userId = 9L;
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

    public ResponseEntity<?> putCalendar(PutCalendarDto dtos) {
        Long myId = 9L;
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

        calendarMapperRepository.deleteAll(mappers);

        List<Long> ids = dtos.getUsers().stream().map(GetUsersAllDto::getId).toList();
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
        calendarMapperRepository.deleteAll(mappers);

        List<CalendarContent> contents = calendarContentRepository.findAllByCalendarAndCalendar_StatusIsNot(calendar.get(),0);
        calendarContentRepository.deleteAll(contents);

        return ResponseEntity.ok("캘린더 삭제가 완료되었습니다.");
    }
}
