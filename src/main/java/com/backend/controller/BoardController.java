package com.backend.controller;

import com.backend.dto.community.BoardDTO;
import com.backend.dto.community.PostDTO;
import com.backend.entity.community.Board;
import com.backend.entity.community.Post;
import com.backend.repository.community.BoardRepository;
import com.backend.service.BoardService;
import com.backend.service.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/community")
public class BoardController {
    private final BoardService boardService;
    private final PostService postService;

    public BoardController(BoardService boardService, PostService postService) {
        this.boardService = boardService;
        this.postService = postService;
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

    //2024/12/20 박연화 추가
    @GetMapping("/board/notice")
    public ResponseEntity<?> getNotice(Authentication auth){
        Long userId = Long.parseLong(auth.getName());
        List<PostDTO> posts = postService.getNotice(userId, Long.valueOf(1));
        if(posts.size() > 0){
            return ResponseEntity.ok().body(posts);
        }else{
            return ResponseEntity.noContent().build();
        }
    }

}
