package com.backend.entity.calendar;

import com.backend.dto.request.calendar.PutCalendarContentDto;
import com.backend.dto.request.calendar.PutCalendarContentsDto;
import com.backend.dto.response.calendar.GetCalendarContentNameDto;
import com.backend.dto.response.calendar.GetCalendarsDto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DialectOverride;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@AllArgsConstructor
@ToString
@NoArgsConstructor
@Builder
@Getter
@Entity
@Table(name = "calendar_content")
public class CalendarContent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "calendar_content_id")
    private Long calendarContentId;

    @Column(name = "calendar_content_status")
    private int status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_id")
    private Calendar calendar;

    @Column(name = "calendar_content_start_date")
    private LocalDateTime calendarStartDate;

    @Column(name = "calendar_content_end_date")
    private LocalDateTime calendarEndDate;

    @Column(name = "calendar_content_name")
    private String name;  // 일정 이름

    @Column(name = "calendar_content_memo")
    private String memo; // 일정 내용

    @Column(name = "calendar_content_alert_status")
    private int alertStatus; // 알람여부

    @Column(name = "calendar_content_location")
    private String location; // 장소지정

    @Column(name = "calendar_content_importance")
    private int importance; // 중요도

    public GetCalendarsDto toGetCalendarsDto() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String sdate = formatter.format(calendarStartDate);
        String edate = formatter.format(calendarEndDate);
        return GetCalendarsDto.builder()
                .title(name)
                .sheave(calendar.getCalendarId())
                .id(calendarContentId)
                .start(sdate)
                .color(calendar.getColor())
                .location(location)
                .importance(importance)
                .memo(memo)
                .alert(alertStatus)
                .end(edate)
                .build();
    }

    public GetCalendarContentNameDto toGetCalendarContentNameDto() {
        String stime = calendarStartDate.format(DateTimeFormatter.ofPattern("HH:mm"));
        return GetCalendarContentNameDto.builder()
                .id(calendarContentId)
                .name(name)
                .color(calendar.getColor())
                .stime(stime)
                .memo(memo)
                .build();
    }

    public void putContents(PutCalendarContentsDto dto) {
        this.name = dto.getTitle();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.calendarStartDate = LocalDateTime.parse(dto.getStartDate(), formatter);
        this.calendarEndDate = LocalDateTime.parse(dto.getEndDate(), formatter);
    }

    public void patchStatus(int i) {
        this.status = i;
    }

    public void putContent(PutCalendarContentDto dto,Calendar calendar) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.name = dto.getTitle();
        this.calendar = calendar;
        this.alertStatus = dto.getAlert();
        this.importance = dto.getImportance();
        this.memo = dto.getMemo();
        this.location = dto.getLocation();
        this.calendarStartDate = LocalDateTime.parse(dto.getSdate(), formatter);
        this.calendarEndDate = LocalDateTime.parse(dto.getEdate(), formatter);
    }

    public void patchContent(PutCalendarContentsDto dto) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.name = dto.getTitle();
        this.calendarStartDate = LocalDateTime.parse(dto.getStartDate(), formatter);
        this.calendarEndDate = LocalDateTime.parse(dto.getEndDate(), formatter);
    }
}
