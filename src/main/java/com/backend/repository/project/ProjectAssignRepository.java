package com.backend.repository.project;

import com.backend.entity.project.ProjectAssign;
import com.backend.entity.project.ProjectTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectAssignRepository extends JpaRepository<ProjectAssign, Long> {
    List<ProjectAssign> findAllByTask(ProjectTask originTask);

    void deleteByTaskId(Long id);
}
