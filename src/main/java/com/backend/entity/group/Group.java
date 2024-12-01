package com.backend.entity.group;

import com.backend.dto.response.GetAdminSidebarGroupsRespDto;
import com.backend.dto.response.group.GetGroupsAllDto;
import com.backend.dto.response.user.GetUsersAllDto;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Builder
@Table(name = "user_group")
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;

    @Column(name = "group_name")
    private String name;  // 그룹 이름

    @OneToMany(mappedBy = "group")
    @ToString.Exclude
    private List<GroupMapper> groupMappers;

    @Column(name = "group_status")
    private int status;  // 부서 관련 상태

    @Column(name = "group_type")
    private Integer type;

    @OneToOne(mappedBy = "group")
    private GroupLeader groupLeader;

    @Column(name = "company")
    private String company;

    public GetAdminSidebarGroupsRespDto toGetAdminSidebarGroupsRespDto() {
        return GetAdminSidebarGroupsRespDto.builder()
                .id(id)
                .name(name)
                .build();
    }

    public void patchGroupName(String update) {
        this.name = update;
    }

    public GetGroupsAllDto toGetGroupsAllDto() {
        Long cnt;
        if(groupMappers.size()==0){
            cnt = 0L;
        } else {
            cnt = (long)groupMappers.size();
        }
//        List<GetUsersAllDto> dtos = new ArrayList<>();
//        for (GroupMapper groupMapper : groupMappers) {
//            GetUsersAllDto dto = groupMapper.getUser().toGetUsersAllDto();
//            dtos.add(dto);
//        }
        return GetGroupsAllDto.builder()
                .id(id)
                .name(name)
                .cnt(cnt)
//                .users(dtos)
                .build();
    }
}
