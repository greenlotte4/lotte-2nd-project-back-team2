package com.backend.repository.calendar;

import com.backend.document.calendar.CalendarAlert;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Repository
public interface CalendarAlertRepository extends MongoRepository<CalendarAlert, String> {
    List<CalendarAlert> findAllByContentId(Long calendarContentId);

    List<CalendarAlert> findAllByCalendarId(Long id);

    List<CalendarAlert> findAllByDate(LocalDate today);

    List<CalendarAlert> findAllByDateBetween(Date start, Date end);

    List<CalendarAlert> findAllByDateBetweenAndTime(Date start, Date end, String hm);
}
