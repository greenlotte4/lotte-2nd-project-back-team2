package com.backend.dto.response.calendar;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
@Builder
public class GetCalendarContentNameDto {
    private String name;
    private String color;
    private String stime;
    private Long id;
    private String memo;
}
