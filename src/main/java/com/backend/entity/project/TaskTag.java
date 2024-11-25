package com.backend.entity.project;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
public class TaskTag {

    @EmbeddedId
    private TaskTagId id;

    @ManyToOne
    @MapsId("taskId")
    @JoinColumn(name = "task_id")
    private ProjectTask task;

    @ManyToOne
    @MapsId("tagId")
    @JoinColumn(name = "tag_id")
    private ProjectTag tag;

    private LocalDateTime createdAt;

    // Getters, setters, equals, hashCode
}

