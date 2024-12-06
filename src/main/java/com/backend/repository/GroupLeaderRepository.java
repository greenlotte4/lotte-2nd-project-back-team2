package com.backend.repository;

import com.backend.entity.group.Group;
import com.backend.entity.group.GroupLeader;
import com.backend.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupLeaderRepository extends JpaRepository<GroupLeader, Long> {
    Optional<GroupLeader> findByGroup(Group group);

    Optional<GroupLeader> findByUser(User user);
}
