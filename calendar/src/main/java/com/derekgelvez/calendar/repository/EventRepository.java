package com.derekgelvez.calendar.repository;

import com.derekgelvez.calendar.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * Events overlapping the requested range: event.start &lt; to AND event.end &gt; from.
     * Also returns the first occurrence of repeating series; the service filters those out
     * and expands series separately.
     */
    List<Event> findByCalendarIdAndStartBeforeAndEndAfter(Long calendarId, LocalDateTime to, LocalDateTime from);

    /** Moves events from one category to another when a category is deleted. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE events SET category_id = :newCategoryId WHERE category_id = :oldCategoryId",
            nativeQuery = true)
    int reassignCategory(@Param("oldCategoryId") Long oldCategoryId, @Param("newCategoryId") Long newCategoryId);
}
