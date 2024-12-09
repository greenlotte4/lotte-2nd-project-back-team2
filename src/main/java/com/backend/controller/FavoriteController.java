package com.backend.controller;

import com.backend.dto.community.FavoriteBoardDTO;
import com.backend.entity.enums.FavoriteType;
import com.backend.service.FavoriteService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Log4j2
@RequestMapping("/api/community/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    // 즐겨찾기 추가
    @PostMapping
    public ResponseEntity<Void> addFavorite(@RequestBody Map<String, Object> payload) {
        Long userId = Long.valueOf((Integer) payload.get("userId"));
        FavoriteType itemType = FavoriteType.valueOf((String) payload.get("itemType"));
        Long itemId = Long.valueOf((Integer) payload.get("itemId"));

        favoriteService.addFavorite(userId, itemType, itemId);
        return ResponseEntity.ok().build();
    }

    // 즐겨찾기 삭제
    @DeleteMapping
    public ResponseEntity<Void> removeFavorite(@RequestParam Long userId, @RequestParam String itemType, @RequestParam Long itemId) {
        favoriteService.removeFavorite(userId, FavoriteType.valueOf(itemType), itemId);
        return ResponseEntity.ok().build();
    }

    // 사용자별 즐겨찾기 목록 조회
    @GetMapping("/{userId}")
    public ResponseEntity getFavorites(@PathVariable Long userId, HttpServletRequest request) {
        String uid = (String)request.getAttribute("uid");
        log.info("uid"+uid);
        List<FavoriteBoardDTO> favorites = favoriteService.getFavoritesByUser(userId);
        log.info("즐겨찾기!!"+favorites.toString());
        return ResponseEntity.ok(favorites);
    }
}
