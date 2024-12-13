package com.backend.service;

import com.backend.dto.community.BoardListResponseDTO;
import com.backend.dto.community.PostDTO;
import com.backend.entity.community.Board;
import com.backend.entity.community.Post;
import com.backend.entity.group.GroupMapper;
import com.backend.entity.user.User;
import com.backend.repository.UserRepository;
import com.backend.repository.community.BoardRepository;
import com.backend.repository.community.PostRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final BoardRepository boardRepository;

    public ResponseEntity<?> createPost(PostDTO post) {
        Optional<Board> board = boardRepository.findById(post.getBoardId());
        String uid = post.getUid();
        Optional<User> user = userRepository.findByUid(uid);
        if (!user.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User userEntity = user.get();
        Post entity = Post.builder()
                .board(board.get())
                .title(post.getTitle())
                .regip(post.getRegip())
                .user(userEntity)
                .isMandatory(post.isMandatory())
                .fileCount(post.getFileCount())
                .writer(userEntity.getName())
                .content(post.getContent())
                .favoritePost(false)
                .build();

        Post postResult = postRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(postResult);
    }


    public ResponseEntity<?> getUid(String uid) {
        Optional<User> user = userRepository.findByUid(uid);

        if (!user.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 UID입니다.");
        }

        User userEntity = user.get();

        String companyCode = userEntity.getCompany();

        List<GroupMapper> groupMappers = userEntity.getGroupMappers();
        log.info("여기까지 오는가 " + companyCode);
        List<Board> boardList = new ArrayList<>();
        if (groupMappers.isEmpty()) {
            boardList = boardRepository.findAllByCompanyAndBoardType(companyCode, 1);
            log.info("그룹 없는 게시판 리스트 " + boardList.toString());
        } else {
            Long groupId = groupMappers.get(0).getGroup().getId();
            boardList = boardRepository.findAllByCompanyAndBoardTypeOrCompanyAndBoardTypeAndGroup_Id
                    (companyCode, 1, companyCode, 2, groupId);
            log.info("그룹 있는 게시판 리스트 " + boardList.toString());
        }
//        return ResponseEntity.status(HttpStatus.OK).body(boardList);
        List<BoardListResponseDTO> boardListDTO = new ArrayList<>();

        for (Board board : boardList) {
            BoardListResponseDTO dto = BoardListResponseDTO.builder()
                    .boardId(board.getBoardId())
                    .boardName(board.getBoardName())
                    .build();
            boardListDTO.add(dto);
        }
        log.info("디티오 리스트 " + boardListDTO.toString());
        return ResponseEntity.status(HttpStatus.OK).body(boardListDTO);
    }


    //    // 게시판 ID로 게시글 목록 조회(페이징처리)
    public List<PostDTO> getPostsByBoardId(Long boardId, Pageable pageable) {
        Page<Post> postsPage = postRepository.findByBoard_BoardIdOrderByCreatedAtAsc(boardId, pageable);

        int currentPage = postsPage.getNumber();  // 현재 페이지 번호

        // Post 엔티티를 PostDTO로 변환
        return postsPage.stream()
                .map(post -> {
                    User userEntity = post.getUser(); // 작성자 정보 가져오기
                    return PostDTO.builder()
                            .postId(post.getPostId())
                            .boardId(post.getBoard().getBoardId())
                            .title(post.getTitle())
                            .content(post.getContent())
                            .writer(userEntity != null ? userEntity.getName() : "Unknown") // User에서 이름 가져오기
                            .createdAt(post.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // 게시물 조회 (단일 게시물 조회, 페이지네이션 필요 없음)
    public PostDTO getPostById(Long boardId, Long postId) {
        try {
            Optional<Post> postOpt = postRepository.findByBoard_BoardIdAndPostId(boardId, postId);
            if (postOpt.isPresent()) {
                Post post = postOpt.get();
                PostDTO postDTO = new PostDTO();
                postDTO.setPostId(post.getPostId());
                postDTO.setTitle(post.getTitle());
                postDTO.setContent(post.getContent());
                postDTO.setUid(post.getUser().getUid());
                postDTO.setRegip(post.getRegip());
                postDTO.setCreatedAt(post.getCreatedAt());

                Board board = post.getBoard();  // Board 객체 가져오기
                if (board != null) {
                    postDTO.setBoardId(board.getBoardId());
                    postDTO.setBoardName(board.getBoardName()); // Board 객체가 null이 아니면 boardName 설정
                } else {
                    log.error("Board 객체가 null입니다.");
                }

                return postDTO;
            } else {
                log.error("게시물을 찾을 수 없습니다.");
                return null;
            }
        } catch (Exception e) {
            log.error("게시물 조회 중 오류 발생: " + e.getMessage(), e);
            throw new RuntimeException("게시물 조회 중 오류 발생", e); // 예외를 처리하고 던짐
        }
    }

    // 부서별 게시판 접근 권한 확인
    public boolean checkBoardAccess(Long boardId, String uid) {
        Optional<Board> boardOpt = boardRepository.findById(boardId);
        if (!boardOpt.isPresent()) {
            return false; // 게시판이 없으면 접근 불가
        }

        Board board = boardOpt.get();

        // 부서별 게시판 접근 권한 확인
        if (board.getBoardType() == 2) {
            return checkUserPermissionForDepartmentBoard(board, uid);
        }
        return true; // 일반 게시판은 접근 허용
    }

    private boolean checkUserPermissionForDepartmentBoard(Board board, String uid) {
        Optional<User> userOpt = userRepository.findByUid(uid);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return user.getGroupMappers().stream()
                    .anyMatch(groupMapper -> groupMapper.getGroup().getId().equals(board.getGroup().getId()));
        }
        return false;
    }
}


