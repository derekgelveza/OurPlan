package com.derekgelvez.calendar.repository;

import com.derekgelvez.calendar.model.EventException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventExceptionRepository extends JpaRepository<EventException, Long> {

    /** Cancelled or edited occurrences to apply when expanding a series. */
    List<EventException> findByEventIdAndOccurrenceDateBetween(Long eventId, LocalDate from, LocalDate to);

    Optional<EventException> findByEventIdAndOccurrenceDate(Long eventId, LocalDate occurrenceDate);

    void deleteByEventId(Long eventId);

    void deleteByEventIdAndOccurrenceDateGreaterThanEqual(Long eventId, LocalDate occurrenceDate);

    /** Moves single-occurrence category overrides when a category is deleted. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE event_exceptions SET category_id = :newCategoryId WHERE category_id = :oldCategoryId",
            nativeQuery = true)
    int reassignCategory(@Param("oldCategoryId") Long oldCategoryId, @Param("newCategoryId") Long newCategoryId);
}
