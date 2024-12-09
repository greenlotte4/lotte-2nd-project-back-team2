package com.backend.repository.community;

import com.backend.entity.community.FavoriteBoard;
import com.backend.entity.enums.FavoriteType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteBoardRepository extends JpaRepository<FavoriteBoard, Long> {
    Optional<List<FavoriteBoard>> findByUserId(Long userId);

    // 특정 사용자가 특정 즐겨찾기 항목을 가지고 있는지 확인
    boolean existsByUserIdAndItemTypeAndItemId(Long userId, FavoriteType itemType, Long itemId);

    // 특정 즐겨찾기 항목 조회
    Optional<FavoriteBoard> findByUserIdAndItemTypeAndItemId(Long userId, FavoriteType itemType, Long itemId);
}
