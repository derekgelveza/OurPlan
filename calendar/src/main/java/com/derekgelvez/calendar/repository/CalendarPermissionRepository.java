package com.derekgelvez.calendar.repository;

import com.derekgelvez.calendar.model.CalendarPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Who has access to a calendar. */
public interface CalendarPermissionRepository extends JpaRepository<CalendarPermission, Long> {

    Optional<CalendarPermission> findByCalendarIdAndUserId(Long calendarId, Long userId);

    boolean existsByCalendarIdAndUserId(Long calendarId, Long userId);

    /** Calendars shared with a user. */
    List<CalendarPermission> findByUserId(Long userId);

    /** Guests of a calendar. */
    List<CalendarPermission> findByCalendarId(Long calendarId);
}
