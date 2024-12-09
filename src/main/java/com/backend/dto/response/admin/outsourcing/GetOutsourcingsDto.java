package com.backend.dto.response.admin.outsourcing;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Getter
@Setter
public class GetOutsourcingsDto {
    private Long id;
    private String name;
    private String hp;
    private int size;
    private int paymentDate;
    private String start;
    private String end;
}
