package com.backend.entity.project;

import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class TaskTagId implements Serializable {
    private Long taskId;
    private Long tagId;

    // Getters, setters, equals, hashCode
}