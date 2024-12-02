package com.backend.entity.calendar;

import com.backend.dto.request.calendar.PutCalendarDto;
import com.backend.dto.response.calendar.GetCalendarContentNameDto;
import com.backend.dto.response.calendar.GetCalendarNameDto;
import com.backend.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Builder
@Table(name = "calendar")
public class Calendar {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "calendar_id")
    private Long calendarId;

    @Column(name = "calendar_status")
    private int status;                     // 0 삭제 1 메인캘린더 2 서브캘린더

    @OneToMany(fetch = FetchType.LAZY,mappedBy = "calendar")
    @ToString.Exclude
    private List<CalendarMapper> calendars;

    @Column(name = "calendar_name")
    private String name;

    @Column(name = "calendar_color")
    private String color;

    @OneToMany(mappedBy = "calendar",fetch = FetchType.EAGER)
    @ToString.Exclude
    private List<CalendarContent> calendarContents = new ArrayList<>();

    public GetCalendarNameDto toGetCalendarNameDto() {
        return GetCalendarNameDto.builder()
                .id(calendarId)
                .name(name)
                .status(status)
                .color(color)
                .build();
    }

    public void patchStatus(int i) {
        this.status = i;
    }

    public void putCalendar(PutCalendarDto dtos) {
        this.status = dtos.getStatus();
        this.color = dtos.getColor();
        this.name = dtos.getName();
    }
}
