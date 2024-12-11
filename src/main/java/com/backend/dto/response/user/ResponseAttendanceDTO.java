package com.backend.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseAttendanceDTO {
    private String id;
    private Long userId;        // 사용자 ID
    private String date;       // 근태 날짜
    private LocalTime checkInTime;   // 출근 시간 (HH:mm 형식)
    private LocalTime checkOutTime;  // 퇴근 시간 (HH:mm 형식)
    private Integer status;
}
