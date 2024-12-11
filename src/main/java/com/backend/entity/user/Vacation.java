package com.backend.entity.user;

import com.backend.dto.request.user.RequestVacationDTO;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDateTime;

/*
    날짜: 2024/12/10
    이름: 박연화
    내용: 연차 테이블 수정
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Getter
@Entity
@Table(name = "vacation")
public class Vacation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vacation_id")
    private Long id;
//    @Column(name = "user_name")
//    private String userName; //연차 신청자 이름
    @Column(name = "user_id")
    private Long userId;  //연차 신청자 Long userid
    @Column(name = "department")
    private Long department;  // 신청자 부서
    @Column(name = "vacation_type")
    private String vacationType; // FULL 연차(8시간) HALF 반차(4시간) 반반차는 없습니다
    @Column(name = "vacation_reason")
    private String reason;  // 연차 사유
    @Column(name = "vacation_sdate")
    private Date startDate; //연차 적용 시작일
    @Column(name = "vacation_edate")
    private Date endDate; //연차 적용 마감일
    @Column(name = "vacation_stime")
    private Time startTime; //연차 적용 시작시간
    @Column(name = "vacation_etime")
    private Time endTime; //연차 적용 마감시간
    @Column(name = "vacation_rdate")
    private LocalDateTime requestDate; // 신청일
    @Column(name = "approveBy")
    private Long approvedBy; //결재자
    @Column(name = "vacation_status")
    private int status; //결재상태 0 대기 1 승인 2 거절

}
