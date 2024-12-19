package com.backend.repository.project;

import com.backend.entity.project.ProjectComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectCommentRepository extends JpaRepository<ProjectComment, Long> {
}
