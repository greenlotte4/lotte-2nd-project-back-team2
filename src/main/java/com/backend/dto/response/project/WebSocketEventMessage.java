package com.backend.dto.response.project;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WebSocketEventMessage {
    private String type; // 이벤트 타입 (TASK_ADDED, TASK_UPDATED, TASK_DELETED 등)
    private Object payload; // 추가 데이터

}
