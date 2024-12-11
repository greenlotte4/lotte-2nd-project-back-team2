package com.backend.repository.user;

import com.backend.document.user.AttendanceTime;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceTimeRepository extends MongoRepository<AttendanceTime, String> {

    Optional<AttendanceTime> findByUserIdAndDate(Long userId, String date);

    List<AttendanceTime> findByDate(String date);

    List<AttendanceTime> findAllByUserIdAndDateBetween(Long userId, String startDate, String endDate);

    List<AttendanceTime> findTop7ByUserIdAndCheckOutTimeIsNotNullOrderByDateDesc(Long userId);

    List<AttendanceTime> findTop14ByUserIdAndCheckOutTimeIsNotNullOrderByDateDesc(Long userId);

    List<AttendanceTime> findTop30ByUserIdAndCheckOutTimeIsNotNullOrderByDateDesc(Long userId);

    @Query("{ 'userId' : :#{#userId}, 'date' : { $gte: :#{#startDate}, $lte: :#{#endDate} }, 'checkOutTime' : { $ne: null } }")
    List<AttendanceTime> findAllByUserIdAndDateBetweenInclusiveAndCheckOutTimeIsNotNull(@Param("userId") Long userId,
                                                                                        @Param("startDate") String startDate,
                                                                                        @Param("endDate") String endDate);
}
