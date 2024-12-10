package com.backend.dto.request.user;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
@Builder
public class PostUserAlarmDto {
    private String id;

    private String date;

    private String time;

    private Integer status;

    private Integer type;

    private String title;

    private String content;

    private String location;

    private String userIds;

    private Long contentId;

    private Long calendarId;
}
