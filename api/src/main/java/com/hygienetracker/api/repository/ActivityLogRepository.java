package com.hygienetracker.api.repository;

import java.time.LocalDate;
import java.util.List;

import com.hygienetracker.api.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Reads and saves logged activities. Showers are activities of type {@code SHOWER}. */
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    /**
     * The local date of each of a user's showers, one entry per shower (two showers on one day
     * give that date twice), which is what {@code ScoreCalculator} expects.
     *
     * @param userId the user whose showers to read
     * @return the dates, in no particular order; empty if the user has never showered
     */
    @Query("""
            SELECT log.localDate FROM ActivityLog log
            WHERE log.user.id = :userId AND log.type.name = 'SHOWER'
            """)
    List<LocalDate> findShowerDatesByUserId(@Param("userId") long userId);
}
