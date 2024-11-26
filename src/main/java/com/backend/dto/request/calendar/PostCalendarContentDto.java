package com.backend.dto.request.calendar;

import lombok.*;
import org.w3c.dom.Text;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
@Builder
public class PostCalendarContentDto {
    private Long title;
    private String sdate;
    private String edate;
    private String stime;
    private String etime;
    private Long calendarId;
    private String location;
    private Integer importance;
    private Integer alert;
    private String memo;
}
