package com.backend.controller;

import com.backend.dto.community.BoardDTO;
import com.backend.entity.community.Board;
import com.backend.repository.community.BoardRepository;
import com.backend.service.BoardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/community")
public class BoardController {
    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    // 전체 게시판 조회
    @GetMapping("/boards")
    public ResponseEntity<List<BoardDTO>> getAllBoards() {
        List<BoardDTO> boards = boardService.getAllBoards();
        return ResponseEntity.ok(boards);
    }

    // 상태별 게시판 조회
    @GetMapping("/boards/status/{status}")
    public ResponseEntity<List<Board>> getBoardsByStatus(@PathVariable int status) {
        List<Board> boards = boardService.getBoardsByStatus(status);
        return ResponseEntity.ok(boards);
    }


    @GetMapping("/boards/group/{groupId}")
    public ResponseEntity<List<Board>> getBoardsByGroup(@PathVariable Long groupId) {
        List<Board> boards = boardService.getBoardsByGroup(groupId);
        return ResponseEntity.ok(boards);
    }

    // 게시판 생성
    @PostMapping("/boards")
    public ResponseEntity<Board> createBoard(@RequestBody Board board) {
        Board newBoard = boardService.createBoard(board);
        return ResponseEntity.ok(newBoard);
    }

}
