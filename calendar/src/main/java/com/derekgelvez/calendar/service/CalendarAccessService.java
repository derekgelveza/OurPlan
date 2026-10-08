package com.derekgelvez.calendar.service;

import com.derekgelvez.calendar.exception.CalendarAccessDeniedException;
import com.derekgelvez.calendar.exception.ResourceNotFoundException;
import com.derekgelvez.calendar.model.AccessLevel;
import com.derekgelvez.calendar.model.Calendar;
import com.derekgelvez.calendar.model.Event;
import com.derekgelvez.calendar.repository.CalendarPermissionRepository;
import com.derekgelvez.calendar.repository.CalendarRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Centralized permission checks used by every event, category and sharing operation.
 * userId is the id of the logged-in user, provided by the authentication layer.
 */
@Service
@Transactional(readOnly = true)
public class CalendarAccessService {

    private final CalendarRepository calendarRepository;
    private final CalendarPermissionRepository calendarPermissionRepository;
    // TEMP-AUTH-DISABLED: This mirrors DISABLE_AUTH so local testing can bypass resource authorization too.
    private final boolean disableAuth;

    public CalendarAccessService(CalendarRepository calendarRepository,
                                 CalendarPermissionRepository calendarPermissionRepository,
                                 @Value("${DISABLE_AUTH:false}") boolean disableAuth) {
        this.calendarRepository = calendarRepository;
        this.calendarPermissionRepository = calendarPermissionRepository;
        this.disableAuth = disableAuth;
    }

    /** Passes if the user owns the calendar or has an active CalendarPermission. */
    public Calendar assertCanView(Long userId, Long calendarId) {
        Calendar calendar = getCalendar(calendarId);
        // TEMP-AUTH-DISABLED: Allow the local test user to view any existing calendar.
        if (disableAuth) {
            return calendar;
        }
        if (!isOwner(userId, calendar) && !calendarPermissionRepository.existsByCalendarIdAndUserId(calendarId, userId)) {
            throw new CalendarAccessDeniedException("You do not have access to this calendar");
        }
        return calendar;
    }

    /** Passes for the owner or a READ_WRITE guest. Used when creating events. */
    public Calendar assertCanEdit(Long userId, Long calendarId) {
        Calendar calendar = getCalendar(calendarId);
        // TEMP-AUTH-DISABLED: Allow end-to-end mutation testing without sharing setup.
        if (disableAuth) {
            return calendar;
        }
        if (!isOwner(userId, calendar) && !isReadWriteGuest(userId, calendarId)) {
            throw new CalendarAccessDeniedException("You cannot edit this calendar");
        }
        return calendar;
    }

    /**
     * Passes for the owner, or for a READ_WRITE guest only when the event was created by
     * that guest. Used when editing or deleting events.
     */
    public void assertCanEdit(Long userId, Long calendarId, Event event) {
        Calendar calendar = getCalendar(calendarId);
        // TEMP-AUTH-DISABLED: Allow editing/deleting any event during local testing.
        if (disableAuth) {
            return;
        }
        if (isOwner(userId, calendar)) {
            return;
        }
        if (isReadWriteGuest(userId, calendarId) && event.getCreatedBy().getId().equals(userId)) {
            return;
        }
        throw new CalendarAccessDeniedException("You cannot modify this event");
    }

    /** Only the owner can manage sharing permissions. */
    public Calendar assertIsOwner(Long userId, Long calendarId) {
        Calendar calendar = getCalendar(calendarId);
        // TEMP-AUTH-DISABLED: Allow sharing-management flows to be exercised anonymously.
        if (disableAuth) {
            return calendar;
        }
        if (!isOwner(userId, calendar)) {
            throw new CalendarAccessDeniedException("Only the calendar owner can do this");
        }
        return calendar;
    }

    public Calendar getCalendar(Long calendarId) {
        return calendarRepository.findById(calendarId)
                .orElseThrow(() -> new ResourceNotFoundException("Calendar " + calendarId + " not found"));
    }

    private boolean isOwner(Long userId, Calendar calendar) {
        return calendar.getOwner().getId().equals(userId);
    }

    private boolean isReadWriteGuest(Long userId, Long calendarId) {
        return calendarPermissionRepository.findByCalendarIdAndUserId(calendarId, userId)
                .map(permission -> permission.getAccessLevel() == AccessLevel.READ_WRITE)
                .orElse(false);
    }
}
