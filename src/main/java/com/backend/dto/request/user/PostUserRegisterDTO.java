package com.backend.dto.request.user;

import com.backend.util.Role;
import lombok.*;

import java.time.LocalDate;
import java.util.Date;

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
//    private String firstName;
//    private String lastName;
    private String country;
    private String addr1;
    private String addr2;
    private Integer grade; // 결제등급 basic company standard enterprise
    private Role role;
    private String day;
    private Long paymentId; //카드인포 아이디
    private String company; //회사코드
    private String companyName; //회사이름
    private int level;
}
