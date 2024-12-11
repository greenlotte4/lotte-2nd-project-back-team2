package com.backend.service;

import com.backend.document.user.AttendanceTime;
import com.backend.dto.request.user.ReqAttendanceDTO;
import com.backend.dto.request.user.RequestVacationDTO;
import com.backend.dto.response.user.ResponseAttendanceDTO;
import com.backend.entity.user.User;
import com.backend.entity.user.Vacation;
import com.backend.repository.UserRepository;
import com.backend.repository.user.AttendanceTimeRepository;
import com.backend.repository.user.VacationRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/*
    날짜: 2024/12/10
    이름: 박연화
    내용: 근태관리
 */

@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class AttendanceService {

    private final AttendanceTimeRepository attendanceTimeRepository;
    private final UserRepository userRepository;
    private final VacationRepository vacationRepository;

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public ResponseEntity<?> goToWork(Long userId) {
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        String date = LocalDate.now(zoneId).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalTime time = LocalTime.now(zoneId);
        Optional<AttendanceTime> optAttendance = attendanceTimeRepository.findByUserIdAndDate(userId, date);

        if (optAttendance.isPresent()) {
            log.info("출근 기록 "+optAttendance.get().toString());
            if(optAttendance.get().getCheckInTime()!=null && optAttendance.get().getCheckOutTime()!=null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("금일 출퇴근 완료된 상태입니다.");
            } else if (optAttendance.get().getCheckInTime()!=null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("출근 기록이 있습니다.");
            }else{
                AttendanceTime attendanceTime = optAttendance.get();
                attendanceTime.setCheckInTime(time, optAttendance.get().getStatus());
                AttendanceTime attendance = attendanceTimeRepository.save(attendanceTime);
                log.info("출근 찍었다 " + attendance);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                String formattedTime = attendance.getCheckInTime().format(formatter);

                return ResponseEntity.ok().body(formattedTime);
            }
        }else {
            int status = 1;
            LocalTime checkTime = LocalTime.parse("09:00:00", DateTimeFormatter.ofPattern("HH:mm:ss"));
            if (time.isAfter(checkTime)) {
                status = 0;
            }

            AttendanceTime entity = AttendanceTime.builder()
                    .userId(userId)
                    .date(date)
                    .status(status)
                    .checkInTime(time)
                    .createAt(LocalDateTime.now())
                    .build();

            AttendanceTime attendance = attendanceTimeRepository.save(entity);
            log.info("출근 찍었다 " + attendance);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            String formattedTime = attendance.getCheckInTime().format(formatter);

            return ResponseEntity.ok().body(formattedTime);
        }
    }

    public ResponseEntity<?> leaveWork(Long userId) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalTime time = LocalTime.now();
        log.info("퇴근 기록 "+date+time);

        Optional<AttendanceTime> optAttendance = attendanceTimeRepository.findByUserIdAndDate(userId, date);
        if(!optAttendance.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("출근 기록이 먼저 입력되어야 합니다.");
        }
        AttendanceTime attendance = optAttendance.get();
        if ( attendance.getCheckOutTime() == null){
            // 퇴근 기록이 없으면 현재 시간으로 설정
            if (attendance.getStatus() == 1) {
                attendance.setCheckOutTime(time, 2);
                attendanceTimeRepository.save(attendance); // 변경 사항 저장
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                String formattedTime = attendance.getCheckInTime().format(formatter);

                return ResponseEntity.ok().body(formattedTime);
            }else{
                attendance.setCheckOutTime(time, attendance.getStatus());
                attendanceTimeRepository.save(attendance); // 변경 사항 저장
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                String formattedTime = attendance.getCheckInTime().format(formatter);

                return ResponseEntity.ok().body(formattedTime);
            }
        }else{
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 퇴근이 완료되었습니다.");
        }

    }

    public void markAttendance( String type) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        List<AttendanceTime> todayRecords = attendanceTimeRepository.findByDate(date);
        log.info("오늘 일자 제대로 뽑히는지 "+todayRecords);
        List<Long> recordedUserIds = todayRecords.stream()
                .map(AttendanceTime::getUserId)
                .toList();
        log.info("제대로 나온 게 맞나? "+recordedUserIds.toString());
        List<User> allUsers = userRepository.findAll();

        // 타입에 따른 처리 분기
        if ("late".equals(type)) {
            handleLate(date, recordedUserIds, allUsers);
        } else if ("absent".equals(type)) {
            handleAbsent(date, recordedUserIds, allUsers);
        }
    }
    private void handleLate(String date, List<Long> recordedUserIds, List<User> allUsers) {
        LocalDate today = LocalDate.now();
        allUsers.stream()
                .filter(user -> !recordedUserIds.contains(user.getId())) // 오늘 기록 없는 사용자
                .filter(user -> !isOnLeave(user.getId(), today)) // 연차가 아닌 사용자
                .forEach(user -> {
                    AttendanceTime lateAttendance = AttendanceTime.builder()
                            .userId(user.getId())
                            .date(date)
                            .checkInTime(null)
                            .checkOutTime(null)
                            .status(0) // LATE
                            .createAt(LocalDateTime.now())
                            .build();
                    attendanceTimeRepository.save(lateAttendance);
                });
    }

    private void handleAbsent(String date, List<Long> recordedUserIds, List<User> allUsers) {
        log.info("전체 유저 데이터 "+allUsers.toString());
        LocalDate today = LocalDate.now();
        allUsers.stream()
                .filter(user -> !recordedUserIds.contains(user.getId())) // 오늘 기록이 없는 사용자
                .filter(user -> !isOnLeave(user.getId(), today)) // 연차가 아닌 사용자만 필터링
                .forEach(user -> {
                    AttendanceTime absentAttendance = AttendanceTime.builder()
                            .userId(user.getId())
                            .date(date)
                            .checkInTime(null)
                            .checkOutTime(null)
                            .status(3) // ABSENT
                            .createAt(LocalDateTime.now())
                            .build();
                    AttendanceTime test = attendanceTimeRepository.save(absentAttendance);
                    log.info("결근 처리 됐나? "+test.toString());
                });
    }

    private boolean isOnLeave(Long userId, LocalDate date) {
        return vacationRepository
                .existsByUserIdAndStartDateAndStatus
                        (userId, date, 1);
    }

    public void markVacation() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate today = LocalDate.now();

        // 오늘 연차 승인을 받은 사용자 조회
        List<Vacation> approvedVacations = vacationRepository.findByStartDateAndStatus(today, 1);
        if (approvedVacations.isEmpty()) {
            log.info("오늘 승인된 연차 사용자가 없습니다.");
            return; // 처리 종료
        }
        // 연차 사용자 ID 목록 추출
        List<Long> vacationUserIds = approvedVacations.stream()
                .map(Vacation::getUserId)
                .toList();

        // 이미 기록된 사용자 확인
        List<AttendanceTime> todayRecords = attendanceTimeRepository.findByDate(date);
        List<Long> recordedUserIds = (todayRecords != null)
                ? todayRecords.stream().map(AttendanceTime::getUserId).toList()
                : Collections.emptyList();

        // 오늘 기록되지 않은 연차 사용자 처리
        vacationUserIds.stream()
                .filter(userId -> !recordedUserIds.contains(userId)) // 중복 방지
                .forEach(userId -> {
                    AttendanceTime vacationAttendance = AttendanceTime.builder()
                            .userId(userId)
                            .date(date)
                            .status(4) // VACATION
                            .checkInTime(null)
                            .checkOutTime(null)
                            .createAt(LocalDateTime.now())
                            .build();
                    AttendanceTime test = attendanceTimeRepository.save(vacationAttendance);
                    log.info("오늘 연차 처리 "+test+LocalDateTime.now());
                });
    }

    public Boolean insertVacation(RequestVacationDTO reqVacationDTO) {
        log.info("연차신청 디티오 "+reqVacationDTO.toString());
        Vacation entity = reqVacationDTO.toEntity();
        log.info("연차신청 엔티티 "+entity.toString());
        Vacation vacation = vacationRepository.save(entity);
        if(vacation == null) {
            return false;
        }else{
            log.info("연차 신청 완료 "+vacation);
            return true;
        }
    }

    public Map<String, String> getTodayAttendance(Long userId) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Optional<AttendanceTime> optAttendance = attendanceTimeRepository.findByUserIdAndDate(userId, date);
        log.info("오늘 출퇴근 ");
        Map<String, String > times = new HashMap<>();
        if(optAttendance.isPresent()) {
            AttendanceTime attendance = optAttendance.get();
            LocalTime checkInTime = attendance.getCheckInTime();
            LocalTime checkOutTime = attendance.getCheckOutTime();
            String start = checkInTime != null ? checkInTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")) : "-";
            String end = checkOutTime != null ? checkOutTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")) : "-";
            times.put("checkInTime",start);
            times.put("checkOutTime",end);
        }else{
            times.put("checkInTime","-");
            times.put("checkOutTime","-");
        }
        log.info("오늘 출퇴근 2 " + times.toString());
        return times;
    }

    public ResponseEntity<?> getWeekAttendance(Long userId, int type) {
        log.info("타입 확인 "+type);
        List<AttendanceTime> attList = new ArrayList<>();
        if (type == 0){
            attList = attendanceTimeRepository
                    .findTop7ByUserIdAndCheckOutTimeIsNotNullOrderByDateDesc(userId);
        }else if (type == 1){
            attList = attendanceTimeRepository
                    .findTop14ByUserIdAndCheckOutTimeIsNotNullOrderByDateDesc(userId);
        }else{
            attList = attendanceTimeRepository
                    .findTop30ByUserIdAndCheckOutTimeIsNotNullOrderByDateDesc(userId);
        }

        if(attList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("근무 기록을 찾을 수 없습니다.");
        }

        log.info("기간별 근태 기록 레포지토리 실행 결과 "+attList.toString());
        List<ResponseAttendanceDTO> dtos = new ArrayList<>();
        attList.forEach(att -> {
            ResponseAttendanceDTO ddd = ResponseAttendanceDTO.builder()
                    .id(att.getId())
                    .userId(att.getUserId())
                    .date(att.getDate())
                    .checkInTime(att.getCheckInTime())
                    .checkOutTime(att.getCheckOutTime())
                    .status(att.getStatus())
                    .build();
            log.info("디티오 변환 과정 "+ddd.toString());
            dtos.add(ddd);
        });
        log.info("근태 검색 디티오 "+dtos.toString());

        return ResponseEntity.ok().body(dtos);
    }

    public ResponseEntity<?> searchByDate(Long uid, ReqAttendanceDTO dto) {
        String start = dto.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String end = dto.getEndDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        List<AttendanceTime> attList = attendanceTimeRepository.findAllByUserIdAndDateBetweenInclusiveAndCheckOutTimeIsNotNull(uid, start, end);
        if(attList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("데이터가 없습니다.");
        }
        log.info("근태 검색 엔티티 "+attList.toString());

        List<ResponseAttendanceDTO> dtos = new ArrayList<>();
        attList.forEach(att -> {
                    ResponseAttendanceDTO ddd = ResponseAttendanceDTO.builder()
                            .id(att.getId())
                            .userId(att.getUserId())
                            .date(att.getDate())
                            .checkInTime(att.getCheckInTime())
                            .checkOutTime(att.getCheckOutTime())
                            .status(att.getStatus())
                            .build();
                    log.info("디티오 변환 과정 "+ddd.toString());
                    dtos.add(ddd);
                });
        log.info("근태 검색 디티오 "+dtos.toString());
        return ResponseEntity.ok().body(dtos);
    }
}
