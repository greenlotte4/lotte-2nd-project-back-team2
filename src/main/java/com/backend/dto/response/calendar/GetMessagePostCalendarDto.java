package com.backend.dto.response.calendar;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
@Builder
public class GetMessagePostCalendarDto {
    private String name;
    private String color;
    private int status;
    private Long id;
}
