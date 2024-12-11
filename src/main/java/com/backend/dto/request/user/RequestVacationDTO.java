package com.backend.dto.request.user;

import com.backend.entity.user.Vacation;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RequestVacationDTO {
//    private String userName; //연차 신청자 이름
    private Long userId;  //연차 신청자 uid
    private Long department;  // 신청자 부서
    private String vacationType; // FULL 연차(8시간) HALF 반차(4시간) 반반차는 없습니다
    private String reason;  // 연차 사유
    private Date startDate; //연차 적용 시작일
    private Date endDate; //연차 적용 마감일
    private Time startTime; //연차 적용 시작시간
    private Time endTime; //연차 적용 마감시간
    private LocalDateTime requestDate; // 신청일
    private Long approvedBy; //결재자
    private int status; //결재상태 0 대기 1 승인 2 거절


    public Vacation toEntity() {
        return Vacation.builder()
                .userId(userId)
                .department(department)
                .vacationType(vacationType)
                .reason(reason)
                .startDate(startDate)
                .endDate(endDate)
                .startTime(startTime)
                .endTime(endTime)
                .requestDate(requestDate)
                .approvedBy(approvedBy)
                .status(status)
                .build();
    }
}
