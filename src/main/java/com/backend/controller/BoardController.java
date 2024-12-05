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

    @GetMapping("/boards")
    public ResponseEntity<List<BoardDTO>> getAllBoards() {
        return ResponseEntity.ok(boardService.getAllBoards());
    }


    @GetMapping("/boards/status/{status}")
    public ResponseEntity<List<Board>> getBoardsByStatus(@PathVariable int status) {
        return ResponseEntity.ok(boardService.getBoardsByStatus(status));
    }

    // 게시판 생성
    @PostMapping("/boards")
    public ResponseEntity<Board> createBoard(@RequestBody Board board) {
        Board newBoard = boardService.createBoard(board);
        return ResponseEntity.ok(newBoard);
    }

    // 부서별 게시판 조회
    @GetMapping("/boards/group/{groupId}")
    public ResponseEntity<Board> getBoardByGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(boardService.getBoardByGroup(groupId));
    }

    @GetMapping("/favorites")
    public ResponseEntity<List<BoardDTO>> getFavoriteBoards() {
        List<BoardDTO> favoriteBoards = boardService.getAllBoards();
        return ResponseEntity.ok(favoriteBoards);
    }
}
