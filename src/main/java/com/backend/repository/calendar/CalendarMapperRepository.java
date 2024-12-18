package com.backend.repository.calendar;

import com.backend.entity.calendar.Calendar;
import com.backend.entity.calendar.CalendarContent;
import com.backend.entity.calendar.CalendarMapper;
import com.backend.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarMapperRepository extends JpaRepository<CalendarMapper,Long> {
    List<CalendarMapper> findAllByUserAndCalendar_StatusIsNot(User user, int i);

    Optional<CalendarMapper> findByUserAndCalendar_Status(User user, int i);

    List<CalendarMapper> findAllByUserAndCalendar(User user, Calendar calendar);

    List<CalendarMapper> findAllByCalendar(Calendar calendar);

    List<CalendarMapper> findByUserAndCalendar_StatusIsNot(User user, int i);

    @Query("""
    SELECT cc FROM CalendarMapper c
    JOIN c.calendar cal
    JOIN cal.calendarContents cc
    WHERE c.user = :user
      AND c.calendar.status != 0
      AND cc.status != 0
      AND (
        (cc.calendarStartDate BETWEEN :startOfTomorrow AND :endOfNextWeek) OR
        (cc.calendarEndDate BETWEEN :startOfTomorrow AND :endOfNextWeek) OR
        (cc.calendarStartDate <= :startOfTomorrow AND cc.calendarEndDate >= :endOfNextWeek)
      )
""")
    List<CalendarContent> findAllContentsWithinDateRange(
            @Param("user") User user,
            @Param("startOfTomorrow") LocalDateTime startOfTomorrow,
            @Param("endOfNextWeek") LocalDateTime endOfNextWeek
    );
}
