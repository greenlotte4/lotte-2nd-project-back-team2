package com.backend.dto.chat;

import lombok.*;

@Getter
@Setter
@ToString
@Builder
public class ApiResponse {
    private String status;
    private Object data;

    public ApiResponse() {}

    public ApiResponse(String status, Object data) {
        this.status = status;
        this.data = data;
    }
}
