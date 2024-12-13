package com.backend.repository;

import com.backend.entity.group.Group;
import com.backend.entity.group.GroupMapper;
import com.backend.entity.user.User;
import com.backend.util.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMapperRepository extends JpaRepository<GroupMapper, Long> {
    List<GroupMapper> findAllByGroup_Name(String team);

    List<GroupMapper> findAllByGroup_NameAndUser_RoleNotIn(String team, List<Role> excludedRoles);

    List<GroupMapper> findAllByGroup_NameAndUser_Status(String team, int i);

    List<GroupMapper> findAllByGroup_NameAndUser_Level(String team, int i);

    List<GroupMapper> findAllByGroup_NameAndUser_LevelOrderByUser_LevelDesc(String team, int i);

    GroupMapper findByUser(User user);

    List<GroupMapper> findAllByGroup(Group group);

    Optional<GroupMapper> findByGroupAndUser(Group group, User user);

    List<GroupMapper> findAllByGroupOrderByUser_LevelDesc(Group group);

    Optional<GroupMapper> findGroupByUserUid(String uid);

     Long countGroupMapperByGroup_Id(Long groupId);

     List<GroupMapper> findGroupMapperByGroup_Id(Long groupId);

}
