package com.backend.dto.request.calendar;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Getter
@Setter
public class PutContentMessageDto {
    private Long calendarId;
    private Long prevId;
}
