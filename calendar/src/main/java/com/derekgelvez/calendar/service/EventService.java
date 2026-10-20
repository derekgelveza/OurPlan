package com.derekgelvez.calendar.service;

import com.derekgelvez.calendar.dto.CategoryResponseDTO;
import com.derekgelvez.calendar.dto.DeleteEventDTO;
import com.derekgelvez.calendar.dto.EventDTO;
import com.derekgelvez.calendar.dto.EventResponseDTO;
import com.derekgelvez.calendar.dto.RecurrenceDTO;
import com.derekgelvez.calendar.dto.UpdateEventDTO;
import com.derekgelvez.calendar.exception.InvalidRequestException;
import com.derekgelvez.calendar.exception.ResourceNotFoundException;
import com.derekgelvez.calendar.model.Calendar;
import com.derekgelvez.calendar.model.Category;
import com.derekgelvez.calendar.model.DeleteScope;
import com.derekgelvez.calendar.model.Event;
import com.derekgelvez.calendar.model.EventException;
import com.derekgelvez.calendar.model.EventRecurrence;
import com.derekgelvez.calendar.model.RecurrenceFrequency;
import com.derekgelvez.calendar.repository.CategoryRepository;
import com.derekgelvez.calendar.repository.EventExceptionRepository;
import com.derekgelvez.calendar.repository.EventRecurrenceRepository;
import com.derekgelvez.calendar.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Event CRUD and recurrence. Range queries are half-open, [from, to), compared on each
 * event's local date-time. Repeating events are expanded on the fly in their own time zone.
 */
@Service
@RequiredArgsConstructor
public class EventService {

    /** Default recurrence end: three months after the start date. */
    static final int DEFAULT_RECURRENCE_MONTHS = 3;

    private final EventRepository eventRepository;
    private final EventRecurrenceRepository eventRecurrenceRepository;
    private final EventExceptionRepository eventExceptionRepository;
    private final CategoryRepository categoryRepository;
    private final CalendarAccessService calendarAccessService;
    private final UserLookup userLookup;

    // ---------------------------------------------------------------- create

    /**
     * Creates a one-time or repeating event. The creator is recorded so READ_WRITE guests can later
     * edit or delete only the events they created.
     */
    @Transactional
    public EventResponseDTO createEvent(Long userId, Long calendarId, EventDTO dto) {
        Calendar calendar = calendarAccessService.assertCanEdit(userId, calendarId);
        Event event = new Event();
        event.setCalendar(calendar);
        event.setCreatedBy(userLookup.getUser(userId));
        applyFields(event, dto, calendar);
        event = eventRepository.save(event);
        EventRecurrence recurrence = dto.recurrence() == null ? null
                : saveRecurrence(new EventRecurrence(), event, dto.recurrence(), null);
        return toResponse(event, recurrence);
    }

    // ---------------------------------------------------------------- read

    /**
     * Every occurrence between from (inclusive) and to (exclusive) for the day, week, or month
     * view, sorted by start.
     */
    @Transactional(readOnly = true)
    public List<EventResponseDTO> getEvents(Long userId, Long calendarId, LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw new InvalidRequestException("to must be after from");
        }
        calendarAccessService.assertCanView(userId, calendarId);

        List<EventRecurrence> series = eventRecurrenceRepository.findSeriesForRange(calendarId, to);
        Set<Long> seriesIds = series.stream().map(r -> r.getEvent().getId()).collect(Collectors.toSet());

        // One-time events come straight from the range query. Repeating events also match it (by their
        // first occurrence), so they are skipped here and expanded separately to avoid duplicates.
        List<EventResponseDTO> occurrences = new ArrayList<>();
        for (Event event : eventRepository.findByCalendarIdAndStartBeforeAndEndAfter(calendarId, to, from)) {
            if (!seriesIds.contains(event.getId())) {
                occurrences.add(toResponse(event, null));
            }
        }
        for (EventRecurrence recurrence : series) {
            occurrences.addAll(expandSeries(recurrence, from, to));
        }
        occurrences.sort(Comparator.comparing(EventResponseDTO::start).thenComparing(EventResponseDTO::eventId));
        return occurrences;
    }

    // ---------------------------------------------------------------- update

    /**
     * Updates an event. For a repeating event the scope says which occurrences the edit applies to:
     * THIS_OCCURRENCE saves an EventException for that date, THIS_AND_FOLLOWING ends the series the
     * day before and starts a new series with the changes, ALL updates the series itself.
     */
    @Transactional
    public EventResponseDTO updateEvent(Long userId, Long eventId, UpdateEventDTO dto) {
        Event event = getEvent(eventId);
        Calendar calendar = event.getCalendar();
        calendarAccessService.assertCanEdit(userId, calendar.getId(), event);
        Optional<EventRecurrence> recurrence = eventRecurrenceRepository.findByEventId(eventId);
        DeleteScope scope = dto.scope() == null ? DeleteScope.ALL : dto.scope();

        // Edit the stored event directly when it doesn't repeat, the edit covers the whole series, or
        // "this and following" starts at the first occurrence (which is the whole series).
        if (recurrence.isEmpty() || scope == DeleteScope.ALL
                || dto.occurrenceDate().equals(event.getStart().toLocalDate()) && scope == DeleteScope.THIS_AND_FOLLOWING) {
            return updateWholeEvent(event, recurrence.orElse(null), dto.toEventDTO(), calendar);
        }

        EventRecurrence rule = recurrence.get();
        LocalDate occurrenceDate = dto.occurrenceDate();
        assertOccurs(rule, occurrenceDate);

        if (scope == DeleteScope.THIS_OCCURRENCE) {
            // Only name, start/end and category can be overridden per occurrence; the request's note,
            // allDay, timeZone and recurrence are not applied to a single occurrence.
            EventException exception = eventExceptionRepository.findByEventIdAndOccurrenceDate(eventId, occurrenceDate)
                    .orElseGet(() -> new EventException(event, occurrenceDate));
            Category category = getCategoryForCalendar(dto.categoryId(), calendar);
            LocalDateTime[] range = normalizeRange(dto.start(), dto.end(), dto.allDay());
            exception.setCancelled(false);
            exception.setName(dto.name().trim());
            exception.setStart(range[0]);
            exception.setEnd(range[1]);
            exception.setCategory(category);
            eventExceptionRepository.save(exception);
            return occurrenceResponse(event, rule, occurrenceDate, exception);
        }

        // THIS_AND_FOLLOWING: end the current series the day before and start a new one.
        // Overrides on or after the split date belonged to the old series and are dropped; the new
        // series keeps the old end date unless the request sets its own.
        LocalDate originalUntil = rule.getUntil();
        rule.setUntil(occurrenceDate.minusDays(1));
        eventRecurrenceRepository.save(rule);
        eventExceptionRepository.deleteByEventIdAndOccurrenceDateGreaterThanEqual(eventId, occurrenceDate);

        Event next = new Event();
        next.setCalendar(calendar);
        next.setCreatedBy(event.getCreatedBy());
        applyFields(next, dto.toEventDTO(), calendar);
        next = eventRepository.save(next);
        EventRecurrence nextRule = dto.recurrence() == null ? null
                : saveRecurrence(new EventRecurrence(), next, dto.recurrence(), originalUntil);
        return toResponse(next, nextRule);
    }

    /** Applies an edit to the stored event, then adds, updates or removes its repeat rule to match. */
    private EventResponseDTO updateWholeEvent(Event event, EventRecurrence existing, EventDTO dto, Calendar calendar) {
        applyFields(event, dto, calendar);
        eventRepository.save(event);
        if (dto.recurrence() == null) {
            if (existing != null) { // stops repeating
                eventExceptionRepository.deleteByEventId(event.getId());
                eventRecurrenceRepository.delete(existing);
            }
            return toResponse(event, null);
        }
        EventRecurrence saved = existing == null // starts repeating, or updates the existing rule
                ? saveRecurrence(new EventRecurrence(), event, dto.recurrence(), null)
                : saveRecurrence(existing, event, dto.recurrence(), existing.getUntil());
        return toResponse(event, saved);
    }

    // ---------------------------------------------------------------- delete

    /**
     * Deletes an event. For a repeating event: THIS_OCCURRENCE cancels that date with an
     * EventException, THIS_AND_FOLLOWING sets the series' until to the day before, and ALL
     * deletes the series and all of its exceptions.
     */
    @Transactional
    public void deleteEvent(Long userId, Long eventId, DeleteEventDTO dto) {
        Event event = getEvent(eventId);
        calendarAccessService.assertCanEdit(userId, event.getCalendar().getId(), event);
        Optional<EventRecurrence> recurrence = eventRecurrenceRepository.findByEventId(eventId);

        // Same rule as update: "this and following" from the first occurrence deletes the whole series.
        boolean deleteAll = recurrence.isEmpty() || dto.scope() == DeleteScope.ALL
                || dto.scope() == DeleteScope.THIS_AND_FOLLOWING
                && dto.occurrenceDate().equals(event.getStart().toLocalDate());
        if (deleteAll) {
            eventExceptionRepository.deleteByEventId(eventId);
            recurrence.ifPresent(eventRecurrenceRepository::delete);
            eventRepository.delete(event);
            return;
        }

        EventRecurrence rule = recurrence.get();
        LocalDate occurrenceDate = dto.occurrenceDate();
        assertOccurs(rule, occurrenceDate);
        if (dto.scope() == DeleteScope.THIS_OCCURRENCE) {
            EventException exception = eventExceptionRepository.findByEventIdAndOccurrenceDate(eventId, occurrenceDate)
                    .orElseGet(() -> new EventException(event, occurrenceDate));
            exception.setCancelled(true);
            eventExceptionRepository.save(exception);
        } else {
            rule.setUntil(occurrenceDate.minusDays(1));
            eventRecurrenceRepository.save(rule);
            eventExceptionRepository.deleteByEventIdAndOccurrenceDateGreaterThanEqual(eventId, occurrenceDate);
        }
    }

    // ---------------------------------------------------------------- expansion

    /** Occurrences of one series that overlap [from, to), with cancellations skipped and overrides applied. */
    private List<EventResponseDTO> expandSeries(EventRecurrence rule, LocalDateTime from, LocalDateTime to) {
        Event event = rule.getEvent();
        Duration duration = Duration.between(event.getStart(), event.getEnd());
        // Include occurrences that began before 'from' but are still running.
        LocalDate windowStart = from.minus(duration).toLocalDate();
        LocalDate windowEnd = to.toLocalDate();

        List<LocalDate> dates = RecurrenceExpander.occurrenceDates(rule.getFrequency(), rule.getInterval(),
                rule.getDaysOfWeek(), event.getStart().toLocalDate(), rule.getUntil(), windowStart, windowEnd);
        if (dates.isEmpty()) {
            return List.of();
        }
        Map<LocalDate, EventException> exceptions = eventExceptionRepository
                .findByEventIdAndOccurrenceDateBetween(event.getId(), dates.getFirst(), dates.getLast()).stream()
                .collect(Collectors.toMap(EventException::getOccurrenceDate, Function.identity()));

        List<EventResponseDTO> result = new ArrayList<>();
        for (LocalDate date : dates) {
            EventException exception = exceptions.get(date);
            if (exception != null && exception.isCancelled()) {
                continue;
            }
            EventResponseDTO occurrence = occurrenceResponse(event, rule, date, exception);
            LocalDateTime storedEnd = event.isAllDay() ? occurrence.end().plusDays(1) : occurrence.end();
            if (occurrence.start().isBefore(to) && storedEnd.isAfter(from)) {
                result.add(occurrence);
            }
        }
        return result;
    }

    /** Same wall-clock time as the first occurrence, shifted only if it falls in a daylight-saving gap. */
    private LocalDateTime occurrenceStart(Event event, LocalDate date) {
        LocalDateTime local = date.atTime(event.getStart().toLocalTime());
        return event.isAllDay() ? local : RecurrenceExpander.resolveLocal(local, event.getTimeZone());
    }

    /** An occurrence of a series on the given date, with any single-occurrence overrides applied. */
    private EventResponseDTO occurrenceResponse(Event event, EventRecurrence rule, LocalDate date,
                                                EventException exception) {
        LocalDateTime start = occurrenceStart(event, date);
        LocalDateTime end = start.plus(Duration.between(event.getStart(), event.getEnd()));
        String name = event.getName();
        Category category = event.getCategory();
        if (exception != null) {
            start = exception.getStart() != null ? exception.getStart() : start;
            end = exception.getEnd() != null ? exception.getEnd() : end;
            name = exception.getName() != null ? exception.getName() : name;
            category = exception.getCategory() != null ? exception.getCategory() : category;
        }
        return response(event, rule, date, name, start, end, category);
    }

    /** A one-time event, or the first occurrence of a series, exactly as stored. */
    private EventResponseDTO toResponse(Event event, EventRecurrence rule) {
        return response(event, rule, event.getStart().toLocalDate(), event.getName(), event.getStart(), event.getEnd(),
                event.getCategory());
    }

    /** rule is null for a one-time event; for a repeating one the response also carries the rule and series start. */
    private static EventResponseDTO response(Event event, EventRecurrence rule, LocalDate occurrenceDate, String name,
                                             LocalDateTime start, LocalDateTime storedEnd, Category category) {
        return new EventResponseDTO(event.getId(), occurrenceDate, name, event.getNote(), start,
                displayEnd(storedEnd, event.isAllDay()), event.isAllDay(), event.getTimeZone(), rule != null,
                CategoryResponseDTO.from(category), RecurrenceDTO.from(rule), event.getStart());
    }

    /** All-day ends are stored exclusive (midnight after the last day) and shown as the inclusive last day. */
    private static LocalDateTime displayEnd(LocalDateTime storedEnd, boolean allDay) {
        return allDay ? storedEnd.minusDays(1) : storedEnd;
    }

    // ---------------------------------------------------------------- helpers

    private void applyFields(Event event, EventDTO dto, Calendar calendar) {
        LocalDateTime[] range = normalizeRange(dto.start(), dto.end(), dto.allDay());
        event.setName(dto.name().trim());
        event.setNote(dto.note());
        event.setStart(range[0]);
        event.setEnd(range[1]);
        event.setAllDay(dto.allDay());
        event.setTimeZone(dto.timeZone());
        event.setCategory(getCategoryForCalendar(dto.categoryId(), calendar));
    }

    /** All-day events use only the dates: start at midnight, end at midnight after the (inclusive) end date. */
    private static LocalDateTime[] normalizeRange(LocalDateTime start, LocalDateTime end, boolean allDay) {
        if (allDay) {
            return new LocalDateTime[]{start.toLocalDate().atStartOfDay(), end.toLocalDate().plusDays(1).atStartOfDay()};
        }
        return new LocalDateTime[]{start, end};
    }

    /** Fills in and saves a recurrence rule (a new one, or the event's existing one when editing the series). */
    private EventRecurrence saveRecurrence(EventRecurrence recurrence, Event event, RecurrenceDTO dto,
                                           LocalDate fallbackUntil) {
        LocalDate startDate = event.getStart().toLocalDate();
        // End date: the request's value, else the fallback (when still valid), else three months after the start.
        LocalDate until = dto.until() != null ? dto.until()
                : fallbackUntil != null && !fallbackUntil.isBefore(startDate) ? fallbackUntil
                : startDate.plusMonths(DEFAULT_RECURRENCE_MONTHS);
        if (until.isBefore(startDate)) {
            throw new InvalidRequestException("until must not be before the event's start date");
        }
        recurrence.setEvent(event);
        recurrence.setFrequency(dto.frequency());
        recurrence.setInterval(dto.interval() == null ? 1 : dto.interval());
        boolean weekly = dto.frequency() == RecurrenceFrequency.WEEKLY || dto.frequency() == RecurrenceFrequency.CUSTOM;
        recurrence.getDaysOfWeek().clear();
        if (weekly) {
            recurrence.getDaysOfWeek().addAll(EnumSet.copyOf(dto.daysOfWeek()));
        }
        recurrence.setUntil(until);
        return eventRecurrenceRepository.save(recurrence);
    }

    /** The category must belong to the calendar owner (guests use the owner's categories). */
    private Category getCategoryForCalendar(Long categoryId, Calendar calendar) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category " + categoryId + " not found"));
        if (!category.getOwner().getId().equals(calendar.getOwner().getId())) {
            throw new InvalidRequestException("The category must belong to the calendar owner");
        }
        return category;
    }

    /** Rejects an edit or delete aimed at a date the series doesn't actually occur on. */
    private void assertOccurs(EventRecurrence rule, LocalDate date) {
        boolean occurs = RecurrenceExpander.occursOn(rule.getFrequency(), rule.getInterval(), rule.getDaysOfWeek(),
                rule.getEvent().getStart().toLocalDate(), rule.getUntil(), date);
        if (!occurs) {
            throw new InvalidRequestException("The event does not occur on " + date);
        }
    }

    private Event getEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event " + eventId + " not found"));
    }
}
