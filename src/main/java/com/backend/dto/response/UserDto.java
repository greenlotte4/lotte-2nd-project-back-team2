package com.backend.dto.response;

import com.backend.entity.group.GroupMapper;
import com.backend.entity.user.Attendance;
import com.backend.entity.user.ProfileImg;
import com.backend.util.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
@Builder
public class UserDto {

    private Long id;
    private Integer status; // 상태
    private String uid; // 유아이디
    private String pwd; // 비밀번호
    private Role role; // 역할 admin user company team 등등 시큐리티에서 쓰는 그거
    private Integer level;
    private Integer grade; // 결제등급 basic company enterprise
    private String email;
    private String hp;
    private String name;
    private String addr1;
    private String country;
    private String addr2;
    private String company;
    private String companyName;
    private Long paymentId; // 결제정보
    private String day;
    @ToString.Exclude
    private List<GroupMapper> groupMappers;
    private LocalDateTime createAt;
    private LocalDateTime lastLogin;
    private LocalDate joinDate;
    private String profileImgPath;
    private String department;
    @ToString.Exclude
    private List<Attendance> attendance;
    private String profileMessage;
    private String levelString;

    //추가필드
    private Long groupId;
    private String permission;

}
