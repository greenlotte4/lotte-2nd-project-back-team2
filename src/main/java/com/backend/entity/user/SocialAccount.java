package com.backend.entity.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_id")
    private Long socialId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "provider")
    private String provider;//카카오, 구글, 네이버

    @Column(name = "create_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

}
