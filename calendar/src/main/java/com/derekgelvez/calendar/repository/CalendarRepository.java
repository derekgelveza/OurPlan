package com.derekgelvez.calendar.repository;

import com.derekgelvez.calendar.model.Calendar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Persistence for user calendars. Calendars shared with a user are found through CalendarPermissionRepository. */
public interface CalendarRepository extends JpaRepository<Calendar, Long> {

    Optional<Calendar> findByOwnerId(Long ownerId);

    boolean existsByOwnerId(Long ownerId);
}
