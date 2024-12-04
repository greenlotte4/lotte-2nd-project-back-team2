package com.backend.service;

import com.backend.dto.request.PostDepartmentReqDto;
import com.backend.dto.response.GetAdminSidebarGroupsRespDto;
import com.backend.dto.response.GetAdminUsersApprovalRespDto;
import com.backend.dto.response.GetAdminUsersDtailRespDto;
import com.backend.dto.response.GetAdminUsersRespDto;
import com.backend.dto.response.group.GetGroupsAllDto;
import com.backend.entity.group.GroupLeader;
import com.backend.entity.group.Group;
import com.backend.entity.group.GroupMapper;
import com.backend.entity.user.User;
import com.backend.repository.GroupLeaderRepository;
import com.backend.repository.GroupMapperRepository;
import com.backend.repository.GroupRepository;
import com.backend.repository.UserRepository;
import com.backend.util.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupService {
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupLeaderRepository groupLeaderRepository;
    private final GroupMapperRepository groupMapperRepository;


    public ResponseEntity<?> postDepartment(PostDepartmentReqDto dto) {
        Optional<Group> department = groupRepository.findByName(dto.getName());

        if(department.isPresent()){
            return ResponseEntity.badRequest().body("Department already exists");
        }
        List<User> users = userRepository.findAllById(dto.getMembers());

        if(users.isEmpty()){
            return ResponseEntity.badRequest().body("No users found");
        }

        Optional<User> user = userRepository.findById(dto.getLeader());

        if(user.isEmpty()){
            return ResponseEntity.badRequest().body("No Leader found");
        }

        Group newGroup = Group.builder()
                .name(dto.getName())
                .status(1)
                .type(0)
                .build();
        groupRepository.save(newGroup);

        List<GroupMapper> mappers = new ArrayList<>();
        users.forEach(v->{
            GroupMapper groupMapper = GroupMapper.builder()
                    .group(newGroup)
                    .user(v)
                    .build();
            mappers.add(groupMapper);
        });

        groupMapperRepository.saveAll(mappers);

        GroupLeader departmentLeader = GroupLeader.builder()
                .group(newGroup)
                .user(user.get())
                .build();
        groupLeaderRepository.save(departmentLeader);

        user.get().updateRole(Role.DEPARTMENT);

        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> postTeam(PostDepartmentReqDto dto) {
        Optional<Group> department = groupRepository.findByName(dto.getName());

        if(department.isPresent()){
            return ResponseEntity.badRequest().body("Department already exists");
        }
        List<User> users = userRepository.findAllById(dto.getMembers());

        if(users.isEmpty()){
            return ResponseEntity.badRequest().body("No users found");
        }

        Optional<User> user = userRepository.findById(dto.getLeader());

        if(user.isEmpty()){
            return ResponseEntity.badRequest().body("No Leader found");
        }

        Group newGroup = Group.builder()
                .name(dto.getName())
                .status(1)
                .type(1)
                .build();
        groupRepository.save(newGroup);
        List<GroupMapper> mappers = new ArrayList<>();
        users.forEach(v->{
            GroupMapper groupMapper = GroupMapper.builder()
                    .group(newGroup)
                    .user(v)
                    .build();
            mappers.add(groupMapper);
        });

        groupMapperRepository.saveAll(mappers);

        GroupLeader departmentLeader = GroupLeader.builder()
                .group(newGroup)
                .user(user.get())
                .build();
        groupLeaderRepository.save(departmentLeader);

        user.get().updateRole(Role.TEAM);

        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> getDepartments() {
        Map<String, Object> resp = new HashMap<>();
        List<Group> departments = groupRepository.findAllByTypeAndStatus(0,1);
        List<GetAdminSidebarGroupsRespDto> dtos = departments.stream().map(Group::toGetAdminSidebarGroupsRespDto).toList();
        resp.put("deps",dtos);
        resp.put("depCnt",dtos.size());
        return ResponseEntity.ok(resp);
    }

    public ResponseEntity<?> getTeams() {
        Map<String, Object> resp = new HashMap<>();
        List<Group> departments = groupRepository.findAllByTypeAndStatus(1,1);
        List<GetAdminSidebarGroupsRespDto> dtos = departments.stream().map(Group::toGetAdminSidebarGroupsRespDto).toList();
        resp.put("teams",dtos);
        resp.put("teamCnt",dtos.size());
        return ResponseEntity.ok(resp);
    }

    public ResponseEntity<?> getLeader(String team) {
        Optional<Group> group = groupRepository.findByName(team);
        if(group.isEmpty()){
            return ResponseEntity.badRequest().body("일치하는 그룹이 없습니다.");
        }
        GroupLeader leader = group.get().getGroupLeader();
        if(leader == null){
            return ResponseEntity.badRequest().body("그룹장이 없습니다.");
        }
        User user = leader.getUser();
        GetAdminUsersRespDto dto = user.toGetAdminUsersRespDto();
        return ResponseEntity.ok(dto);
    }

    public ResponseEntity<?> patchLeader(Long id, String name) {
        Optional<Group> group = groupRepository.findByName(name);
        if(group.isEmpty()){
            return ResponseEntity.badRequest().body("해당하는 그룹이 존재하지않습니다.");
        }
        GroupLeader leader = group.get().getGroupLeader();
        Optional<User> user = userRepository.findById(id);
        if(user.isEmpty()) {
            return ResponseEntity.badRequest().body("해당하는 유저가 존재하지않습니다.");
        }
        if(leader == null){
            GroupLeader newGroupLeader = GroupLeader.builder()
                    .group(group.get())
                    .user(user.get())
                    .build();
            groupLeaderRepository.save(newGroupLeader);
            if(group.get().getType()==1){
                user.get().updateRole(Role.TEAM);
            } else {
                user.get().updateRole(Role.DEPARTMENT);
            }
        } else {
            User oldLeader = leader.getUser();
            oldLeader.updateRole(Role.WORKER);
            userRepository.save(oldLeader);
            if(group.get().getType()==1){
                user.get().updateRole(Role.TEAM);
            } else {
                user.get().updateRole(Role.DEPARTMENT);
            }
            leader.patchLeader(user.get());
        }
        return ResponseEntity.ok("변경완료했습니다.");
    }

    public ResponseEntity<?> getGroupMembers(String team) {
        List<GroupMapper> groupMappers = groupMapperRepository.findAllByGroup_Name(team);
        if(groupMappers.isEmpty()){
            return ResponseEntity.badRequest().body("소속 인원이 존재하지 않습니다.");
        }
        List<GetAdminUsersRespDto> dtos = groupMappers.stream().map(GroupMapper::toGetAdminUsersRespDto).toList();

        return ResponseEntity.ok(dtos);
    }

    public ResponseEntity<?> patchGroupMembers(List<Long> ids, String team) {
        List<Role> excludedRoles = Arrays.asList(Role.TEAM, Role.DEPARTMENT);
        List<GroupMapper> groupMappers = groupMapperRepository.findAllByGroup_NameAndUser_RoleNotIn(team, excludedRoles);
        groupMapperRepository.deleteAll(groupMappers);
        Group group = groupMappers.get(0).getGroup();
        List<GroupMapper> newGroupMappers = new ArrayList<>();
        ids.forEach(v->{
            Optional<User> user = userRepository.findById(v);
            GroupMapper groupMapper = GroupMapper.builder()
                    .group(group)
                    .user(user.get())
                    .build();
            newGroupMappers.add(groupMapper);
        });
        groupMapperRepository.saveAll(newGroupMappers);

        return ResponseEntity.ok().body("구성원이 변경되었습니다.");
    }

    public ResponseEntity<?> patchGroupName(String name, String update) {
        Optional<Group> group = groupRepository.findByName(name);
        if(group.isEmpty()){
            return ResponseEntity.badRequest().body("그룹이 없습니다.");
        }
        group.get().patchGroupName(update);
        if(group.get().getType()==0){
            return ResponseEntity.ok().body("부서명이 "+update+"로 변경되었습니다.");
        } else {
            return ResponseEntity.ok().body("팀명이 "+update+"로 변경되었습니다.");
        }

    }

    public ResponseEntity<?> getGroupMembersDetail(String team) {
        List<User> users = new ArrayList<>();
        Optional<Group> group = groupRepository.findByName(team);
        if(group.isEmpty()){
            return ResponseEntity.badRequest().body("해당 그룹이 존재하지 않습니다.");
        }
        List<GroupMapper> groupMappers = groupMapperRepository.findAllByGroup_Name(team);
        if(groupMappers.isEmpty()){
            if(group.get().getType()==0){
                return ResponseEntity.badRequest().body("해당 부서에 소속인원이 없습니다.");
            } else {
                return ResponseEntity.badRequest().body("해당 팀에 소속인원이 없습니다.");
            }
        }
        User leader = group.get().getGroupLeader().getUser();
        if(leader == null){
            if(group.get().getType()==0){
                return ResponseEntity.badRequest().body("해당 부서에 부서장이 없습니다.");
            } else {
                return ResponseEntity.badRequest().body("해당 팀에 팀장이 없습니다.");
            }
        }
        users.add(leader);
        groupMappers.forEach(v->{
            User user=v.getUser();
            users.add(user);
        });

        List<GetAdminUsersDtailRespDto> dtos = users.stream().map(User::toGetAdminUsersDtailRespDto).toList();
        return ResponseEntity.ok(dtos);
    }

    public ResponseEntity<?> getGroupMembersApproval() {
        List<User> users = userRepository.findAllByStatus(0);
        if(users.isEmpty()){
            return ResponseEntity.ok("승인 요청이 없습니다.");
        }
        List<GetAdminUsersApprovalRespDto> dtos = users.stream().map(User::toGetAdminUsersApprovalRespDto).toList();
        return ResponseEntity.ok(dtos);
    }

    public ResponseEntity<?> getGroupMembersDetailBySearch(String team, String condition, String keyword) {
        List<User> users = new ArrayList<>();
        Optional<Group> group = groupRepository.findByName(team);
        if(group.isEmpty()){
            return ResponseEntity.badRequest().body("해당 그룹이 존재하지 않습니다.");
        }
        List<GroupMapper> groupMappers = groupMapperRepository.findAllByGroup_NameAndUser_LevelOrderByUser_LevelDesc(team,Integer.parseInt(keyword));
        if(groupMappers.isEmpty()){
            if(group.get().getType()==0){
                return ResponseEntity.badRequest().body("해당 부서에 검색결과가 없습니다.");
            } else {
                return ResponseEntity.badRequest().body("해당 팀에 검색결과가 없습니다.");
            }
        }
        groupMappers.forEach(v->{
            User user=v.getUser();
            users.add(user);
        });

        List<GetAdminUsersDtailRespDto> dtos = users.stream().map(User::toGetAdminUsersDtailRespDto).toList();
        return ResponseEntity.ok(dtos);
    }

    public Page<GetGroupsAllDto> getGroupsAll(int page) {
        Pageable pageable = PageRequest.of(page,5);
        Page<Group> groups = groupRepository.findAllByCompanyAndStatusIsNot("1246857",0,pageable);
        Page<GetGroupsAllDto> dtos = groups.map(Group::toGetGroupsAllDto);
        return dtos;
    }

    public Page<GetGroupsAllDto> getGroupsAllByKeyword(int page, String keyword) {
        Pageable pageable = PageRequest.of(page,5);
        Page<Group> groups = groupRepository.findAllByCompanyAndNameContainingAndStatusIsNot("1246857",keyword,0,pageable);
        Page<GetGroupsAllDto> dtos = groups.map(Group::toGetGroupsAllDto);
        return dtos;
    }

    //12.02 전규찬 findGroupNameByUser 추가
    public String findGroupNameByUser(User user) {
        GroupMapper groupMapper = groupMapperRepository.findByUser(user);
        return groupMapper.getGroup().getName();
    }

    public List<GroupMapper> findAllGroupMappers() {
        List<GroupMapper> groupMappers = groupMapperRepository.findAll();
        if(groupMappers.isEmpty()){
            return groupMappers;
        } else {
            return groupMappers;
        }
    }

}
