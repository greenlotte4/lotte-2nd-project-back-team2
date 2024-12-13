package com.backend.repository.user;

import com.backend.entity.user.ProfileImg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileImgRepository extends JpaRepository<ProfileImg, Long> {
    void deleteByUserId(Long userId);
}
