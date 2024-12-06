package com.backend.repository.project;

import com.backend.entity.project.Project;
import com.backend.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByCoworkers_User(User leader);

    List<Project> findAllByCoworkers_UserAndStatusIsNot(User leader, int i);
}
