package com.backend.document.calendar;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Builder
public class CalendarAlert {

    @Id
    private String id;

    @Column(name = "alert_date")
    private LocalDate date;

    @Column(name = "alert_time")
    private String time;

    @Column(name = "alert_status")
    private Integer status;

    @Column(name = "alert_type")
    private Integer type;

    @Column(name = "alert_title")
    private String title;

    @Column(name = "alert_content")
    private String content;

    @Column(name = "alert_location")
    private String location;

    @Column(name = "userIds")
    private String userIds;

    @Column(name = "content_id")
    private Long contentId;

    @Column(name = "calendar_id")
    private Long calendarId;

    public void patchUsers(String newIds) {
        this.userIds = newIds;
    }
}
