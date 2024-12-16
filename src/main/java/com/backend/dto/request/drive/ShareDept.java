package com.backend.dto.request.drive;

import lombok.*;

@ToString
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareDept {
    private String deptId;
    private String deptName;
    private String permission; // 읽기, 수정 등 권한
    private int cnt; // 부서 구성원 수 (선택적으로 추가)
}
