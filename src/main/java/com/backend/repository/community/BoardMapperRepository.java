package com.backend.repository.community;

/*
    날짜 : 2024/12/03
    이름 : 박서홍
    내용 : BoardMapperRepository 생성
 */

import com.backend.entity.community.BoardMapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardMapperRepository extends JpaRepository<BoardMapper, Integer> {




}
