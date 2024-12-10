package com.backend.entity.user;

import jakarta.persistence.*;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Builder
public class Alert {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "alarm_title")
    private String title;

    @Column(name = "alarm_content")
    private String content;

    @Column(name = "alarm_createAt")
    private String createAt;

    @Column(name = "alert_status")
    private Integer status;  //0 삭제 , 1 읽음 , 2 읽지않음
}
