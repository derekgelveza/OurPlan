package com.derekgelvez.calendar.controller;

import com.derekgelvez.calendar.config.CurrentUserProvider;
import com.derekgelvez.calendar.dto.DeleteEventDTO;
import com.derekgelvez.calendar.dto.EventDTO;
import com.derekgelvez.calendar.dto.EventResponseDTO;
import com.derekgelvez.calendar.dto.UpdateEventDTO;
import com.derekgelvez.calendar.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/** Exposes event CRUD endpoints, including recurrence-related requests, and delegates to EventService. */
@RestController
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final CurrentUserProvider currentUserProvider;

    /** Every occurrence in [from, to), e.g. ?from=2026-10-05T00:00&to=2026-10-12T00:00 for a week view. */
    @GetMapping("/calendars/{calendarId}/events")
    public List<EventResponseDTO> getEvents(
            @PathVariable Long calendarId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return eventService.getEvents(currentUserProvider.getCurrentUserId(), calendarId, from, to);
    }

    @PostMapping("/calendars/{calendarId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponseDTO createEvent(@PathVariable Long calendarId, @Valid @RequestBody EventDTO dto) {
        return eventService.createEvent(currentUserProvider.getCurrentUserId(), calendarId, dto);
    }

    @PutMapping("/events/{eventId}")
    public EventResponseDTO updateEvent(@PathVariable Long eventId, @Valid @RequestBody UpdateEventDTO dto) {
        return eventService.updateEvent(currentUserProvider.getCurrentUserId(), eventId, dto);
    }

    @DeleteMapping("/events/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvent(@PathVariable Long eventId, @Valid @RequestBody DeleteEventDTO dto) {
        eventService.deleteEvent(currentUserProvider.getCurrentUserId(), eventId, dto);
    }
}
