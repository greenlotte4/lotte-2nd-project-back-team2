package com.backend.service;

import com.backend.dto.community.FavoriteBoardDTO;
import com.backend.entity.community.Board;
import com.backend.entity.community.FavoriteBoard;
import com.backend.entity.enums.FavoriteType;
import com.backend.entity.user.User;
import com.backend.repository.UserRepository;
import com.backend.repository.community.BoardRepository;
import com.backend.repository.community.FavoriteBoardRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    private final FavoriteBoardRepository favoriteBoardRepository;
    private final UserRepository userRepository;
    private final BoardRepository boardRepository;

    public FavoriteService(FavoriteBoardRepository favoriteBoardRepository, UserRepository userRepository, BoardRepository boardRepository) {
        this.favoriteBoardRepository = favoriteBoardRepository;
        this.userRepository = userRepository;
        this.boardRepository = boardRepository;
    }

    @Transactional
    public void addFavorite(Long userId, FavoriteType itemType, Long itemId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (favoriteBoardRepository.existsByUserIdAndItemTypeAndItemId(userId, itemType, itemId)) {
            throw new IllegalArgumentException("이미 즐겨찾기됨 ");
        }

        // FavoriteBoard 생성 및 저장
        FavoriteBoard favoriteBoard = FavoriteBoard.builder()
                .user(user) // 조회한 User 객체 설정
                .itemType(itemType)
                .itemId(itemId)
                .build();

        favoriteBoardRepository.save(favoriteBoard);
    }

    public void removeFavorite(Long userId, FavoriteType itemType, Long itemId) {
        FavoriteBoard favoriteBoard = favoriteBoardRepository.findByUserIdAndItemTypeAndItemId(userId, itemType, itemId)
                .orElseThrow(() -> new IllegalArgumentException("즐겨찾기 항목이 존재하지 않습니다."));

        favoriteBoardRepository.delete(favoriteBoard);
    }

    // 특정 사용자의 즐겨찾기 목록 조회
    public List<FavoriteBoardDTO> getFavoritesByUser(Long userId) {
        Optional<List<FavoriteBoard>> opt = favoriteBoardRepository.findByUserId(userId);
        if (!opt.isPresent()) {
            return Collections.emptyList(); // 데이터가 없을 경우 빈 리스트 반환
        }
        List<FavoriteBoard> favoriteBoards = opt.get();
        // 엔티티를 DTO로 변환
        return favoriteBoards.stream()
                .map(favorite -> {
                            FavoriteBoardDTO favoriteBoardDTO = new FavoriteBoardDTO(
                                    favorite.getItemId(),
                                    favorite.getItemType().name(),
                                    getItemName(favorite.getItemType(), favorite.getItemId()) // 게시판 또는 게시글 이름 조회
                            );
                            return favoriteBoardDTO;
                        }
                )
                .collect(Collectors.toList());
    }

    // 게시판 또는 게시글 이름 조회 로직
    private String getItemName(FavoriteType itemType, Long itemId) {
        if (itemType == FavoriteType.BOARD) {
            return boardRepository.findById(itemId)
                    .map(Board::getBoardName)
                    .orElse("게시판 이름을 찾을 수 없음");
        }
        return "알 수 없음";
    }
}
