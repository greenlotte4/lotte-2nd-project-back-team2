package com.backend.repository.community;
/*
    날짜 : 2024/12/03
    이름 : 박서홍
    내용 : BoardRepository 생성
 */


import com.backend.entity.community.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    List<Board> findAllByOrderByBoardNameAsc();
    List<Board> findByStatus(int status); // 특정 유형의 게시판 조회
    boolean existsByBoardName(String boardName); // 게시판 이름 중복 여부 확인
    List<Board> findByGroup_Id(Long groupId); // 부서(Group ID)로 게시판 조회

    List<Board> findAllByCompanyAndBoardTypeOrCompanyAndBoardTypeAndGroup_Id(
            String company1, int boardType1,
            String company2, int boardType2, Long groupId
    );

    List<Board> findAllByCompanyAndBoardType(String companyCode, int boardType);
}
