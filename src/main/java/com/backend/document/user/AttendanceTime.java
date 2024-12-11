package com.backend.document.user;

/*
    날짜 : 2024.12.09
    이름 : 박연화
    내용 : 일별 근태관리 데이터 저장
 */

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Builder
@Document(collection = "attendance_time")
public class AttendanceTime {

    @Id
    private String id;            // MongoDB 기본 키
    private Long userId;        // 사용자 ID
    private String date;       // 근태 날짜
    private LocalTime checkInTime;   // 출근 시간 (HH:mm 형식)
    private LocalTime checkOutTime;  // 퇴근 시간 (HH:mm 형식)
    private Integer status;       // 출퇴근 상태 0 지각 1 출근 (근무중) 2 퇴근(정상근무 완료) 3 결근 4 연차 5 반차 후 출근

    @CreatedDate
    private LocalDateTime createAt;   // 생성일자
    @LastModifiedDate
    private LocalDateTime updateAt;   // 마지막 수정일자

    public void setCheckOutTime(LocalTime time, int status) {
        this.checkOutTime = time;
        this.updateAt = LocalDateTime.now();
        this.status = status;
    }

    public void setCheckInTime(LocalTime time, int status) {
        this.checkInTime = time;
        this.updateAt = LocalDateTime.now();
        this.status = status;
    }

}
