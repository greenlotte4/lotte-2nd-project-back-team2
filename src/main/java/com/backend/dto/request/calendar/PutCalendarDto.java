package com.backend.dto.request.calendar;

import com.backend.dto.response.user.GetUsersAllDto;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Setter
@Getter
public class PutCalendarDto {
    private String name;
    private int status;
    private String color;
    private List<GetUsersAllDto> users;
    private Long id;
}
