package com.backend.entity.community;


import com.backend.entity.enums.FavoriteType;
import com.backend.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Entity
public class FavoriteBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_id")
    private Long favoriteId;

    @Enumerated(EnumType.STRING) // Enum을 문자열로 매핑
    @Column(name = "item_type", nullable = false)
    private FavoriteType itemType; // 즐겨찾기 대상 타입 (BOARD or POST)

    @Column(name = "item_id", nullable = false)
    private Long itemId; // 즐겨찾기 대상의 ID (게시판 ID 또는 게시글 ID)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // User 테이블의 외래 키
    private User user;

}
