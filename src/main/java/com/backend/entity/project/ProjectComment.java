package com.backend.entity.project;

import com.backend.dto.response.project.GetProjectCommentDTO;
import com.backend.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Builder
@Entity
@Table(name = "project_comment")
public class ProjectComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @Setter
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "task_id")
    private ProjectTask task;

    private String user_id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private String content;

    @CreationTimestamp
    private LocalDateTime rdate;

    public GetProjectCommentDTO toDTO() {
        return GetProjectCommentDTO.builder()
                .id(id)
                .user_id(user_id)
                .content(content)
                .rdate(rdate)
                .build();
    }
}