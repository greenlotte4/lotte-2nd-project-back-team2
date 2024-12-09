package com.backend.service;

/*
    날짜 : 2024/12/03
    이름 : 박서홍
    내용 : BoardService 생성
 */

import com.backend.dto.community.BoardDTO;
import com.backend.entity.community.Board;
import com.backend.repository.community.BoardRepository;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BoardService {
    private final BoardRepository boardRepository;

    public BoardService(BoardRepository boardRepository) {
        this.boardRepository = boardRepository;
    }

    public List<BoardDTO> getAllBoards() {
        return boardRepository.findAllByOrderByBoardNameAsc()
                .stream()
                .map(board -> {
                    if (board.getGroup() != null) {
                        Hibernate.initialize(board.getGroup()); // 명시적 초기화
                    }
                    return new BoardDTO(
                            board.getBoardId(),
                            board.getStatus(),
                            board.getGroup() != null ? board.getGroup().getId() : null,
                            board.getBoardName(),
                            board.getDescription(),
                            board.isFavoriteBoard(),
                            board.getCreatedAt(),
                            board.getUpdatedAt()
                    );
                })
                .collect(Collectors.toList());
    }

    public List<Board> getBoardsByStatus(int status) {
        return boardRepository.findByStatus(status);
    }

    public Board createBoard(Board board) {
        if (boardRepository.existsByBoardName(board.getBoardName())) {
            throw new IllegalArgumentException("중복된 게시판 이름입니다.");
        }
        return boardRepository.save(board);
    }


    // 부서별 게시판 조회
    public List<Board> getBoardsByGroup(Long groupId) {
        System.out.println("Received groupId: " + groupId);
        List<Board> boards = boardRepository.findByGroup_Id(groupId);
        System.out.println("Query result: " + boards);
        return boards;
    }
}
