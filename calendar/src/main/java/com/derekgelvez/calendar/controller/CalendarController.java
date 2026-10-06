package com.derekgelvez.calendar.controller;

import com.derekgelvez.calendar.config.CurrentUserProvider;
import com.derekgelvez.calendar.dto.CalendarResponseDTO;
import com.derekgelvez.calendar.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Exposes calendar endpoints and delegates to CalendarService. */
@RestController
@RequestMapping("/calendars")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;
    private final CurrentUserProvider currentUserProvider;

    /** Creates the logged-in user's calendar (and their "Uncategorized" category). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CalendarResponseDTO createCalendar() {
        return calendarService.createCalendar(currentUserProvider.getCurrentUserId());
    }

    /** The user's own calendar and the calendars shared with them. */
    @GetMapping
    public List<CalendarResponseDTO> getCalendars() {
        return calendarService.getCalendars(currentUserProvider.getCurrentUserId());
    }
}
