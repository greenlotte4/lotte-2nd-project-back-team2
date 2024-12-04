package com.backend.repository.calendar;

import com.backend.entity.calendar.Calendar;
import com.backend.entity.calendar.CalendarContent;
import com.backend.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarContentRepository extends JpaRepository<CalendarContent, Long> {
    Optional<CalendarContent> findByCalendarContentId(Long contentId);

    List<CalendarContent> findAllByCalendarAndCalendar_StatusIsNot(Calendar calendar, int i);

    List<CalendarContent> findAllByCalendar_CalendarIdAndCalendar_StatusIsNot(long l, int i);

    List<CalendarContent> findAllByCalendarAndCalendar_StatusIsNotAndStatusIsNot(Calendar calendar, int i, int i1);

    Optional<CalendarContent> findByCalendarContentIdAndStatusIsNot(Long contentId, int i);
}
