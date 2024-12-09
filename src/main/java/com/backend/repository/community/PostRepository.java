package com.backend.repository.community;

import com.backend.entity.community.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // 특정 게시판의 게시글 조회
    List<Post> findByBoard_BoardId(Long boardId);

    // 특정 작성자의 게시글 조회
    List<Post> findByUser_Id(Long userId);


}
