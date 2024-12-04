package com.backend.repository.calendar;

import com.backend.entity.calendar.Calendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, Long> {

    Optional<Calendar> findByCalendarIdAndStatusIsNot(Long calendarId, int i);

    Optional<Calendar> findByCalendarId(Long calendarId);

    Optional<Calendar> findByCalendarContents_CalendarContentId(Long contentId);

    Optional<Calendar> findByCalendarIdAndCalendarContents_StatusIsNot(long l, int i);
}
