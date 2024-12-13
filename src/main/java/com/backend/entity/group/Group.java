package com.backend.entity.group;

import com.backend.dto.response.GetAdminSidebarGroupsRespDto;
import com.backend.dto.response.admin.group.GetGroupDto;
import com.backend.dto.response.group.GetGroupsAllDto;
import com.backend.dto.response.user.GetUsersAllDto;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Builder
@Table(name = "`user_group`")
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;

    @Column(name = "group_name")
    private String name;  // 그룹 이름

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private List<GroupMapper> groupMappers;

    @Column(name = "group_status")
    private int status;  // 부서 관련 상태

    @Column(name = "group_type")
    private Integer type;

    @OneToOne(mappedBy = "group")
    private GroupLeader groupLeader;

    @Column(name = "company")
    private String company;

    @Column(name = "group_description")
    private String description;

    @Column(name = "group_link")
    private Integer link;

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

    public void putData(GetGroupDto dto) {
        this.name = dto.getName();
        this.description = dto.getDescription();
        this.link = dto.getLink();
    }

    public void patchStatus(int i) {
        this.status = i;
    }
}
