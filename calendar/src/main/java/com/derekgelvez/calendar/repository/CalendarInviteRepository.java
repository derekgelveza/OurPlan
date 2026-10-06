package com.derekgelvez.calendar.repository;

import com.derekgelvez.calendar.model.CalendarInvite;
import com.derekgelvez.calendar.model.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CalendarInviteRepository extends JpaRepository<CalendarInvite, Long> {

    /** Finds the invite when someone opens the invite link. */
    Optional<CalendarInvite> findByToken(String token);

    /** Lists a calendar's invites with the given status, e.g. PENDING. */
    List<CalendarInvite> findByCalendarIdAndStatus(Long calendarId, InviteStatus status);
}
