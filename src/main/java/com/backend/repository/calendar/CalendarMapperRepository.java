package com.backend.repository.calendar;

import com.backend.entity.calendar.Calendar;
import com.backend.entity.calendar.CalendarMapper;
import com.backend.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarMapperRepository extends JpaRepository<CalendarMapper,Long> {
    List<CalendarMapper> findAllByUserAndCalendar_StatusIsNot(User user, int i);

    Optional<CalendarMapper> findByUserAndCalendar_Status(User user, int i);

    List<CalendarMapper> findAllByUserAndCalendar(User user, Calendar calendar);

    List<CalendarMapper> findAllByCalendar(Calendar calendar);

    List<CalendarMapper> findByUserAndCalendar_StatusIsNot(User user, int i);

}
