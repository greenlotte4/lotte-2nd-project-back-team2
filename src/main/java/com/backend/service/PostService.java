package com.backend.service;

import com.backend.dto.community.BoardListResponseDTO;
import com.backend.dto.community.PostDTO;
import com.backend.entity.community.Board;
import com.backend.entity.community.Post;
import com.backend.entity.group.GroupMapper;
import com.backend.entity.user.User;
import com.backend.repository.UserRepository;
import com.backend.repository.community.BoardRepository;
import com.backend.repository.community.CommentRepository;
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
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final CommentRepository commentRepository;

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


        // 게시판 ID로 게시글 목록 조회(페이징처리)
    public List<PostDTO> getPostsByBoardId(Long boardId, Pageable pageable) {
        Page<Post> postsPage = postRepository.findByBoard_BoardIdOrderByCreatedAtDesc(boardId, pageable);

        int currentPage = postsPage.getNumber();  // 현재 페이지 번호

        // Post 엔티티를 PostDTO로 변환
        return postsPage.stream()
                .map(post -> {
                    User userEntity = post.getUser();
                    log.info("Post ID: " + post.getPostId());
                    log.info("User Entity: " + (userEntity != null ? userEntity.getName() : "User is null"));

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

                PostDTO postDTO = PostDTO.builder()
                        .postId(post.getPostId())
                        .title(post.getTitle())
                        .content(post.getContent())
                        .writer(post.getWriter()) // Post 엔티티의 writer 필드에서 가져오기
                        .uid(post.getUser() != null ? post.getUser().getUid() : null)
                        .regip(post.getRegip())
                        .boardId(post.getBoard().getBoardId())
                        .boardName(post.getBoard().getBoardName())
                        .createdAt(post.getCreatedAt())
                        .build();

                return postDTO;
            } else {
                log.error("게시물을 찾을 수 없습니다.");
                return null;
            }
        } catch (Exception e) {
            log.error("게시물 조회 중 오류 발생: " + e.getMessage(), e);
            throw new RuntimeException("게시물 조회 중 오류 발생", e);
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

    @Transactional
    public void updatePost(Long boardId, Long postId, PostDTO postUpdateDTO) {
        // 게시글 조회
        Post post = postRepository.findByBoard_BoardIdAndPostId(boardId, postId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));

        // 제목과 내용 업데이트
        post.setTitle(postUpdateDTO.getTitle());
        post.setContent(postUpdateDTO.getContent());

        // 파일이 존재할 경우 파일 처리
        MultipartFile file = postUpdateDTO.getFile();
        if (file != null && !file.isEmpty()) {
            try {
                String fileName = file.getOriginalFilename();
                post.setFileName(fileName);
                log.info("파일이 업데이트되었습니다: {}", fileName);

                // 실제 파일 저장 로직은 필요에 따라 구현 (예: 파일 시스템, 클라우드 스토리지 등)
            } catch (Exception e) {
                log.error("파일 업로드 중 오류 발생:", e);
                throw new RuntimeException("파일 업로드 실패", e);
            }
        }

        // 수정된 게시글 저장
        postRepository.save(post);
        log.info("게시글이 수정되었습니다: postId = {}", postId);
    }


    @Transactional
    public void deletePost(Long boardId, Long postId) {
        Post post = postRepository.findByBoard_BoardIdAndPostId(boardId, postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
        commentRepository.deleteByPost_PostId(postId);

        postRepository.delete(post);
    }

    // 게시판 ID로 게시글 목록 조회(페이징처리)
    public List<PostDTO> getNotice(Long boardId) {
        List<Post> postsPage = postRepository.findTop4ByBoard_BoardIdOrderByCreatedAtDesc(boardId);

        // Post 엔티티를 PostDTO로 변환
        return postsPage.stream()
                .map(post -> {
                    User userEntity = post.getUser();
                    log.info("Post ID: " + post.getPostId());
                    log.info("User Entity: " + (userEntity != null ? userEntity.getName() : "User is null"));

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

}


