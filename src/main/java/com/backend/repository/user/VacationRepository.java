package com.backend.repository.user;

import com.backend.entity.user.Vacation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VacationRepository extends JpaRepository<Vacation, Integer> {

    boolean existsByUserIdAndStartDateAndStatus(
            Long userId, LocalDate date, int status);
    List<Vacation> findByStartDateAndStatus(LocalDate today, int status);
}
