package com.backend.dto.request.admin.outsourcing;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Getter
@Setter
public class PostOutsourcingDto {
    private String hp;
    private int payment;
    private String end;
    private int size;
    private String outsourcingName;
}
