package com.backend.dto.request.email;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancellationRequestDto {
    private String title;
    private String name;
    private String email;
    private String orderNumber;
    private String productName;
    private String returnReason;
    private String content;
} 