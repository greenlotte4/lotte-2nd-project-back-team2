package com.backend.entity.community;

import com.backend.entity.calendar.Calendar;
import com.backend.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Builder
@Table(name = "board_mapper")
public class BoardMapper extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_mapper_id")
    private Long id; // 권한의 고유 ID

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 권한을 가진 사용자

    @ManyToOne
    @JoinColumn(name = "board_id", nullable = false) // 게시판이 반드시 있어야 함
    private Board board ; //

    @Column(nullable = false)
    private int canRead;

    @Column(nullable = false)
    private int canWrite;

    @Column(nullable = false)
    private int canDelete;
}


