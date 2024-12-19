package com.backend.repository.project;

import com.backend.entity.project.ProjectColumn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectColumnRepository extends JpaRepository<ProjectColumn, Long> {
    List<ProjectColumn> findAllByProjectId(Long projectId);
}
