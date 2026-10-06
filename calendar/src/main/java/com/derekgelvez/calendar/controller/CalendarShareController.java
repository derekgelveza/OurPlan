package com.derekgelvez.calendar.controller;

import com.derekgelvez.calendar.config.CurrentUserProvider;
import com.derekgelvez.calendar.dto.CalendarInviteResponseDTO;
import com.derekgelvez.calendar.dto.CalendarPermissionResponseDTO;
import com.derekgelvez.calendar.dto.CalendarResponseDTO;
import com.derekgelvez.calendar.dto.ShareCalendarRequestDTO;
import com.derekgelvez.calendar.dto.ShareCalendarResponseDTO;
import com.derekgelvez.calendar.service.CalendarShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Exposes calendar sharing endpoints (invites and guest access) and delegates to CalendarShareService. */
@RestController
@RequiredArgsConstructor
public class CalendarShareController {

    private final CalendarShareService calendarShareService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/calendars/{calendarId}/share")
    @ResponseStatus(HttpStatus.CREATED)
    public ShareCalendarResponseDTO shareCalendar(@PathVariable Long calendarId,
                                                  @Valid @RequestBody ShareCalendarRequestDTO dto) {
        return calendarShareService.shareCalendar(currentUserProvider.getCurrentUserId(), calendarId, dto);
    }

    @GetMapping("/calendars/{calendarId}/invites")
    public List<CalendarInviteResponseDTO> getPendingInvites(@PathVariable Long calendarId) {
        return calendarShareService.getPendingInvites(currentUserProvider.getCurrentUserId(), calendarId);
    }

    @GetMapping("/calendars/{calendarId}/permissions")
    public List<CalendarPermissionResponseDTO> getPermissions(@PathVariable Long calendarId) {
        return calendarShareService.getPermissions(currentUserProvider.getCurrentUserId(), calendarId);
    }

    /** The owner revokes a guest's access, or a guest removes themselves by passing their own id. */
    @DeleteMapping("/calendars/{calendarId}/permissions/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePermission(@PathVariable Long calendarId, @PathVariable Long userId) {
        calendarShareService.removePermission(currentUserProvider.getCurrentUserId(), calendarId, userId);
    }

    @PostMapping("/invites/{token}/accept")
    public CalendarResponseDTO acceptInvite(@PathVariable String token) {
        return calendarShareService.acceptInvite(currentUserProvider.getCurrentUserId(), token);
    }

    @PostMapping("/invites/{token}/decline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void declineInvite(@PathVariable String token) {
        calendarShareService.declineInvite(currentUserProvider.getCurrentUserId(), token);
    }
}
