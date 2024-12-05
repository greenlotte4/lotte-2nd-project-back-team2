package com.backend.dto.request.user;

import com.backend.util.Role;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostUserRegisterDTO {
    private Long id;
    private String uid; // 유아이디
    private String pwd; // 비밀번호
    private String email;
    private String hp;
    private String name;
    private String country;
    private String addr1;
    private String addr2;
    private Integer grade; // 결제등급 basic company standard enterprise
    private Role role;
    private Long paymentId;
    private PaymentInfoDTO paymentInfo;
    private String companyCode;
}
