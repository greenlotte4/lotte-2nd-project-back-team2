package com.backend.dto.request.email;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PaymentRequestDto {
    private String title;
    private String name;
    private String email;
    private String orderNumber;
    private Long paymentAmount;
    private String paymentMethod;
    private String paymentDate;
    private String inquiryType;
    private String content;
} 