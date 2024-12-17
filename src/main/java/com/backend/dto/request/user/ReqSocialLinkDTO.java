package com.backend.dto.request.user;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReqSocialLinkDTO {
    private Long socialId;
    private Long userId;
    private String providerId;
    private String provider;//카카오, 구글, 네이버
    private LocalDateTime createdAt;
}
