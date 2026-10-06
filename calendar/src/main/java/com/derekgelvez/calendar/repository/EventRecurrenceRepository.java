package com.derekgelvez.calendar.repository;

import com.derekgelvez.calendar.model.EventRecurrence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRecurrenceRepository extends JpaRepository<EventRecurrence, Long> {

    Optional<EventRecurrence> findByEventId(Long eventId);

    /**
     * Repeating series of a calendar that start before the end of the range. Series cannot be
     * found with a simple date query, so the service expands their occurrences (and applies
     * until) itself.
     */
    @Query("select r from EventRecurrence r join fetch r.event e "
            + "where e.calendar.id = :calendarId and e.start < :to")
    List<EventRecurrence> findSeriesForRange(@Param("calendarId") Long calendarId, @Param("to") LocalDateTime to);
}
