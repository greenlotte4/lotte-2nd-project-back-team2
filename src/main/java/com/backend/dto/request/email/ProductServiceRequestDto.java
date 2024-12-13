package com.backend.dto.request.email;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductServiceRequestDto {
    private String title;
    private String name;
    private String email;
    private String productName;
    private String productType;
    private String serviceType;
    private String purchaseDate;
    private String content;
} 