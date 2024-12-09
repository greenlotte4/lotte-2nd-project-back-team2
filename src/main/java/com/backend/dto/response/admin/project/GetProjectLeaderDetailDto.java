package com.backend.dto.response.admin.project;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Getter
@Setter
public class GetProjectLeaderDetailDto {
    private Long id;
    private String hp;
    private String name;
    private String address;
    private String calendarName;
    private Long calendarId;
    private List<GetProjects> projects;
    private String level;
    private List<String> groupsNames;
}


