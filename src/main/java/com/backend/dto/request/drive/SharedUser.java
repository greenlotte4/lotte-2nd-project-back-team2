package com.backend.dto.request.drive;


import com.backend.util.Role;
import lombok.*;

@ToString
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SharedUser {
    private Long id;
    private String name;
    private String email;
    private String group;
    private String uid;
    private String authority;
    private String permission; // 읽기, 수정 등 권한
    private String profile;
}
