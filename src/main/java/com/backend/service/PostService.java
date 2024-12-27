package com.backend.service;

import com.backend.document.BoardFile;
import com.backend.document.drive.FileMogo;
import com.backend.dto.community.BoardListResponseDTO;
import com.backend.dto.community.PostDTO;
import com.backend.dto.request.FileRequestDto;
import com.backend.entity.community.Board;
import com.backend.entity.community.Post;
import com.backend.entity.group.GroupMapper;
import com.backend.entity.user.User;
import com.backend.repository.UserRepository;
import com.backend.repository.community.BoardFileRepository;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.server.UID;
import java.time.LocalDateTime;
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
    private final SftpService sftpService;
    private final FolderService folderService;
    private final BoardFileRepository boardFileRepository;

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

        // 게시글 저장
        Post postResult = postRepository.save(entity);

        // 업로드 파일 개수 제한 검사
        List<MultipartFile> files = post.getFiles();
        if (files.size() > 2) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("파일은 최대 2개까지만 업로드할 수 있습니다.");
        }

        // SFTP 업로드 처리 및 MongoDB 저장
        String remoteDir = "uploads/board/" + uid; // 사용자별 디렉토리
        List<BoardFile> savedFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String originalFilename = file.getOriginalFilename();
                String savedFilename = folderService.generateSavedName(originalFilename);
                String remoteFilePath = remoteDir + "/" + savedFilename;

                File tempFile = null;
                try {
                    tempFile = File.createTempFile("upload_", "_" + originalFilename);
                    file.transferTo(tempFile);

                    // SFTP 파일 업로드
                    String uploadedPath = sftpService.uploadFile(
                            tempFile.getAbsolutePath(),
                            remoteDir,
                            savedFilename
                    );

                    // 업로드된 파일 경로 확인
                    log.info("파일 업로드 경로: {}", uploadedPath);

                    // MongoDB에 파일 메타데이터 저장
                    BoardFile boardFile = BoardFile.builder()
                            .postId(postResult.getPostId())
                            .originalName(originalFilename)
                            .savedName(savedFilename)
                            .path(remoteDir + "/" + savedFilename)  // 경로 설정
                            .ownerUid(uid)
                            .size(file.getSize())
                            .createdAt(LocalDateTime.now())
                            .build();

                    savedFiles.add(boardFile);
                    boardFileRepository.save(boardFile);

                } catch (IOException e) {
                    log.error("임시 파일 생성 또는 전송 중 오류 발생: {}", e.getMessage());
                } finally {
                    if (tempFile != null && tempFile.exists()) {
                        if (tempFile.delete()) {
                            log.info("임시 파일 삭제 성공: {}", tempFile.getAbsolutePath());
                        } else {
                            log.warn("임시 파일 삭제 실패: {}", tempFile.getAbsolutePath());
                        }
                    }
                }
            }
        }

        log.info("모든 파일 업로드 및 메타데이터 저장 완료");
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
                            .hit(post.getHit())
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

                // 조회수 증가
                post.setHit(post.getHit() + 1);
                postRepository.save(post); // 조회수 업데이트 저장

                PostDTO postDTO = PostDTO.builder()
                        .postId(post.getPostId())
                        .title(post.getTitle())
                        .content(post.getContent())
                        .writer(post.getWriter()) // Post 엔티티의 writer 필드에서 가져오기
                        .uid(post.getUser() != null ? post.getUser().getUid() : null)
                        .regip(post.getRegip())
                        .boardId(post.getBoard().getBoardId())
                        .hit(post.getHit())
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
    public void updatePost(Long boardId, Long postId, PostDTO postUpdateDTO, List<MultipartFile> files) {
        // 게시글 조회
        Post post = postRepository.findByBoard_BoardIdAndPostId(boardId, postId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));

        // 기본 정보 업데이트
        post.setTitle(postUpdateDTO.getTitle());
        post.setContent(postUpdateDTO.getContent());

        // 게시판 변경 처리
        if (postUpdateDTO.getBoardId() != null && !postUpdateDTO.getBoardId().equals(boardId)) {
            Board newBoard = boardRepository.findById(postUpdateDTO.getBoardId())
                    .orElseThrow(() -> new IllegalArgumentException("해당 게시판이 존재하지 않습니다."));
            post.setBoard(newBoard);
        }

        // 파일 업로드 처리
        if (files != null && !files.isEmpty()) {
            // 업로드 파일 개수 제한 검사
            if (files.size() > 2) {
                throw new IllegalArgumentException("파일은 최대 2개까지만 업로드할 수 있습니다.");
            }

            String uid = postUpdateDTO.getUid();
            String remoteDir = "uploads/board/" + uid; // 사용자별 디렉토리

            // 기존 파일 삭제
            List<BoardFile> existingFiles = boardFileRepository.findByPostId(postId);
            for (BoardFile existingFile : existingFiles) {
                try {
                    // SFTP에서 파일 삭제
                    sftpService.delete(existingFile.getPath());
                    // MongoDB에서 파일 메타데이터 삭제
                    boardFileRepository.delete(existingFile);
                } catch (Exception e) {
                    log.error("기존 파일 삭제 중 오류 발생: {}", e.getMessage());
                }
            }

            // 새 파일 업로드
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String originalFilename = file.getOriginalFilename();
                    String savedFilename = folderService.generateSavedName(originalFilename);
                    String remoteFilePath = remoteDir + "/" + savedFilename;

                    File tempFile = null;
                    try {
                        tempFile = File.createTempFile("upload_", "_" + originalFilename);
                        file.transferTo(tempFile);

                        // SFTP 파일 업로드
                        String uploadedPath = sftpService.uploadFile(
                                tempFile.getAbsolutePath(),
                                remoteDir,
                                savedFilename
                        );

                        log.info("파일 업로드 경로: {}", uploadedPath);

                        // MongoDB에 파일 메타데이터 저장
                        BoardFile boardFile = BoardFile.builder()
                                .postId(postId)
                                .originalName(originalFilename)
                                .savedName(savedFilename)
                                .path(remoteDir + "/" + savedFilename)
                                .ownerUid(uid)
                                .size(file.getSize())
                                .createdAt(LocalDateTime.now())
                                .build();

                        boardFileRepository.save(boardFile);

                    } catch (IOException e) {
                        log.error("파일 업로드 중 오류 발생: {}", e.getMessage());
                        throw new RuntimeException("파일 업로드 실패", e);
                    } finally {
                        if (tempFile != null && tempFile.exists()) {
                            if (tempFile.delete()) {
                                log.info("임시 파일 삭제 성공: {}", tempFile.getAbsolutePath());
                            } else {
                                log.warn("임시 파일 삭제 실패: {}", tempFile.getAbsolutePath());
                            }
                        }
                    }
                }
            }

            post.setFileCount(files.size());
        }

        // 게시글 저장
        postRepository.save(post);
        log.info("게시글 수정 완료 - postId: {}", postId);
    }
    @Transactional
    public void deletePost(Long boardId, Long postId) {
        Post post = postRepository.findByBoard_BoardIdAndPostId(boardId, postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
        commentRepository.deleteByPost_PostId(postId);

        postRepository.delete(post);
    }

    // 게시판 ID로 게시글 목록 조회(페이징처리)
    public List<PostDTO> getNotice(Long userId, Long boardId) {
        List<Post> postsPage = postRepository.findTop5ByBoard_BoardIdOrderByCreatedAtDesc(boardId);
        
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
                            .hit(post.getHit())
                            .comment(post.getCommentCount())
                            .isMandatory(post.isMandatory())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public Page<PostDTO> getFilteredPosts(Long boardId, String keyword, Pageable pageable) {
        log.info("Filtering posts for boardId: {}, keyword: '{}', pageable: {}", boardId, keyword, pageable);

        Page<Post> posts = postRepository.searchPosts(boardId, keyword, pageable);

        log.info("Number of posts found: {}", posts.getTotalElements());

        return posts.map(post -> PostDTO.builder()
                .postId(post.getPostId())
                .boardId(post.getBoard().getBoardId())
                .title(post.getTitle())
                .content(post.getContent())
                .writer(post.getWriter())
                .createdAt(post.getCreatedAt())
                .build());
    }

    public List<BoardFile> getFilesByPostId(Long postId) {
        return boardFileRepository.findByPostId(postId);
    }
    public boolean downloadFile(String remoteDir, String remoteFileName, String localFilePath) {
        return sftpService.downloadFile(remoteDir, remoteFileName, localFilePath);
    }
}





