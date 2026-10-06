package com.derekgelvez.calendar.service;

import com.derekgelvez.calendar.model.RecurrenceFrequency;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Calculates the dates a repeating event occurs on. Occurrences are never stored.
 * <ul>
 *   <li>The series begins on the event's start date; {@code until} is inclusive.</li>
 *   <li>WEEKLY and CUSTOM both use {@code daysOfWeek}; interval counts weeks
 *       (Monday-based) from the week of the start date.</li>
 *   <li>MONTHLY and YEARLY are always counted from the original start date. When the
 *       target day does not exist (e.g. the 31st in February, or Feb 29 in a non-leap
 *       year) the occurrence falls on the last valid day of that month, and later
 *       occurrences return to the original day.</li>
 * </ul>
 */
public final class RecurrenceExpander {

    private RecurrenceExpander() {
    }

    /**
     * Occurrence dates of the series within [windowStart, windowEnd] (both inclusive),
     * in ascending order.
     */
    public static List<LocalDate> occurrenceDates(RecurrenceFrequency frequency, int interval,
                                                  Set<DayOfWeek> daysOfWeek, LocalDate seriesStart,
                                                  LocalDate until, LocalDate windowStart, LocalDate windowEnd) {
        int step = Math.max(1, interval);
        LocalDate last = until == null || until.isAfter(windowEnd) ? windowEnd : until;
        LocalDate first = windowStart.isBefore(seriesStart) ? seriesStart : windowStart;
        List<LocalDate> dates = new ArrayList<>();
        if (first.isAfter(last)) {
            return dates;
        }
        switch (frequency) {
            case DAILY -> {
                long k = Math.ceilDiv(ChronoUnit.DAYS.between(seriesStart, first), step);
                for (LocalDate d = seriesStart.plusDays(k * step); !d.isAfter(last); d = d.plusDays(step)) {
                    dates.add(d);
                }
            }
            case WEEKLY, CUSTOM -> {
                Set<DayOfWeek> days = daysOfWeek == null || daysOfWeek.isEmpty()
                        ? EnumSet.of(seriesStart.getDayOfWeek())
                        : EnumSet.copyOf(daysOfWeek);
                LocalDate seriesWeek = seriesStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate firstWeek = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                long k = Math.ceilDiv(ChronoUnit.WEEKS.between(seriesWeek, firstWeek), step);
                for (LocalDate week = seriesWeek.plusWeeks(k * step); !week.isAfter(last); week = week.plusWeeks(step)) {
                    for (DayOfWeek day : days) { // EnumSet iterates Monday..Sunday
                        LocalDate d = week.plusDays(day.getValue() - 1L);
                        if (!d.isBefore(first) && !d.isAfter(last)) {
                            dates.add(d);
                        }
                    }
                }
            }
            case MONTHLY -> addCalendarSteps(dates, seriesStart, first, last, step, ChronoUnit.MONTHS);
            case YEARLY -> addCalendarSteps(dates, seriesStart, first, last, step, ChronoUnit.YEARS);
        }
        return dates;
    }

    /** Whether the series has an occurrence on the given date. */
    public static boolean occursOn(RecurrenceFrequency frequency, int interval, Set<DayOfWeek> daysOfWeek,
                                   LocalDate seriesStart, LocalDate until, LocalDate date) {
        return !occurrenceDates(frequency, interval, daysOfWeek, seriesStart, until, date, date).isEmpty();
    }

    /**
     * Local date-time of an occurrence in the event's time zone. Keeps the intended wall-clock
     * time across daylight-saving changes; a time that does not exist on that day (spring-forward
     * gap) is moved forward by the length of the gap.
     */
    public static LocalDateTime resolveLocal(LocalDateTime local, String timeZone) {
        return ZonedDateTime.of(local, ZoneId.of(timeZone)).toLocalDateTime();
    }

    private static void addCalendarSteps(List<LocalDate> dates, LocalDate seriesStart, LocalDate first,
                                         LocalDate last, int step, ChronoUnit unit) {
        // Start one step early so a clamped (end-of-month) date just before 'first' is not skipped.
        long k = Math.max(0, unit.between(seriesStart, first) / step - 1);
        while (true) {
            LocalDate d = seriesStart.plus(k * step, unit); // clamps to the last valid day
            if (d.isAfter(last)) {
                return;
            }
            if (!d.isBefore(first)) {
                dates.add(d);
            }
            k++;
        }
    }
}
