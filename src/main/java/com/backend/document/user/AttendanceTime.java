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

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Builder
@Document(collection = "attendance_time")
public class AttendanceTime {

    @Id
    private String id;            // MongoDB 기본 키
    private String userId;        // 사용자 ID
    private LocalDate date;       // 근태 날짜
    private LocalTime checkInTime;   // 출근 시간 (HH:mm 형식)
    private LocalTime checkOutTime;  // 퇴근 시간 (HH:mm 형식)
    private Integer status;       // 출퇴근 상태 0 결근 1 출근 (근무중) 2 퇴근

    @CreatedDate
    private LocalDateTime createAt;   // 생성일자
    @LastModifiedDate
    private LocalDateTime updateAt;   // 마지막 수정일자
}
