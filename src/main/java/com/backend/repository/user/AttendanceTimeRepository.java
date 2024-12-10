package com.backend.repository.user;

import com.backend.document.user.AttendanceTime;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceTimeRepository extends MongoRepository<AttendanceTime, String> {

    Optional<AttendanceTime> findByUserIdAndDate(String uid, String date);

    List<AttendanceTime> findByDate(String date);
}
