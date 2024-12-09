package com.backend.dto.chat;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UnreadCountDTO {
    private long unreadCount;
}
