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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    public List<Post> findByBoard_BoardId(Long boardId) {
        return postRepository.findByBoard_BoardId(boardId);
    }

    public ResponseEntity<?> getUid(String uid) {
        Optional<User> user = userRepository.findByUid(uid);

        if (!user.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 UID입니다.");
        }

        User userEntity = user.get();

        String companyCode = userEntity.getCompany();

        List<GroupMapper> groupMappers = userEntity.getGroupMappers();
        log.info("여기까지 오는가 "+companyCode);
        List<Board> boardList = new ArrayList<>();
        if(groupMappers.isEmpty()){
            boardList = boardRepository.findAllByCompanyAndBoardType(companyCode, 1);
            log.info("그룹 없는 게시판 리스트 "+boardList.toString());
        }else{
            Long groupId = groupMappers.get(0).getGroup().getId();
            boardList = boardRepository.findAllByCompanyAndBoardTypeOrCompanyAndBoardTypeAndGroup_Id
                    (companyCode,1,companyCode,2,groupId);
            log.info("그룹 있는 게시판 리스트 "+boardList.toString());
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
        log.info("디티오 리스트 "+boardListDTO.toString());
        return ResponseEntity.status(HttpStatus.OK).body(boardListDTO);
    }


}



