package com.backend.repository.user;

import com.backend.entity.user.CardInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardInfoRepository extends JpaRepository<CardInfo, Long> {
    List<CardInfo> findAllByUserId(Long userId);
}
