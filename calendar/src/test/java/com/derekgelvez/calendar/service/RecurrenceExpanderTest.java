package com.derekgelvez.calendar.service;

import com.derekgelvez.calendar.model.RecurrenceFrequency;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.derekgelvez.calendar.model.RecurrenceFrequency.CUSTOM;
import static com.derekgelvez.calendar.model.RecurrenceFrequency.DAILY;
import static com.derekgelvez.calendar.model.RecurrenceFrequency.MONTHLY;
import static com.derekgelvez.calendar.model.RecurrenceFrequency.WEEKLY;
import static com.derekgelvez.calendar.model.RecurrenceFrequency.YEARLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecurrenceExpanderTest {

    @Test
    void dailyOccurrencesInsideWindow() {
        assertEquals(dates("2026-10-05", "2026-10-06", "2026-10-07"),
                expand(DAILY, 1, null, "2026-10-01", null, "2026-10-05", "2026-10-07"));
    }

    @Test
    void dailyIntervalStaysAlignedToStartDate() {
        assertEquals(dates("2026-10-07", "2026-10-09"),
                expand(DAILY, 2, null, "2026-10-01", null, "2026-10-06", "2026-10-10"));
    }

    @Test
    void untilIsInclusiveAndCutsTheWindow() {
        assertEquals(dates("2026-10-01"), expand(DAILY, 1, null, "2026-10-01", "2026-10-01", "2026-09-01", "2026-12-01"));
        assertEquals(dates("2026-10-05", "2026-10-06"),
                expand(DAILY, 1, null, "2026-10-01", "2026-10-06", "2026-10-05", "2026-10-20"));
    }

    @Test
    void nothingBeforeTheSeriesStarts() {
        assertEquals(List.of(), expand(DAILY, 1, null, "2026-10-10", null, "2026-10-01", "2026-10-09"));
    }

    @Test
    void weeklyOnSelectedDays() {
        assertEquals(dates("2026-10-06", "2026-10-08", "2026-10-13", "2026-10-15"),
                expand(WEEKLY, 1, EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
                        "2026-10-06", null, "2026-10-01", "2026-10-16"));
    }

    @Test
    void customEveryOtherWeekSkipsDaysBeforeTheStart() {
        Set<DayOfWeek> monWed = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
        // Series starts Wednesday 2026-10-07, so Monday 2026-10-05 is not an occurrence.
        assertEquals(dates("2026-10-07", "2026-10-19", "2026-10-21"),
                expand(CUSTOM, 2, monWed, "2026-10-07", null, "2026-10-01", "2026-10-25"));
        assertEquals(dates("2026-11-02", "2026-11-04"),
                expand(CUSTOM, 2, monWed, "2026-10-07", null, "2026-10-26", "2026-11-08"));
    }

    @Test
    void monthlyOnThe31stFallsOnLastDayOfShorterMonths() {
        assertEquals(dates("2027-01-31", "2027-02-28", "2027-03-31", "2027-04-30"),
                expand(MONTHLY, 1, null, "2027-01-31", null, "2027-01-01", "2027-04-30"));
        assertEquals(dates("2027-02-28"), expand(MONTHLY, 1, null, "2027-01-31", null, "2027-02-28", "2027-02-28"));
    }

    @Test
    void monthlyInterval() {
        assertEquals(dates("2027-03-15", "2027-06-15"),
                expand(MONTHLY, 3, null, "2026-12-15", null, "2027-01-01", "2027-07-01"));
    }

    @Test
    void yearlyOnLeapDay() {
        assertEquals(dates("2025-02-28", "2026-02-28", "2027-02-28", "2028-02-29"),
                expand(YEARLY, 1, null, "2024-02-29", null, "2025-01-01", "2028-12-31"));
    }

    @Test
    void occursOnRespectsWeekdayAndUntil() {
        LocalDate start = LocalDate.parse("2026-10-05");
        Set<DayOfWeek> monday = EnumSet.of(DayOfWeek.MONDAY);
        LocalDate until = start.plusMonths(3);
        assertTrue(RecurrenceExpander.occursOn(WEEKLY, 1, monday, start, until, LocalDate.parse("2027-01-04")));
        assertFalse(RecurrenceExpander.occursOn(WEEKLY, 1, monday, start, until, LocalDate.parse("2027-01-11")));
        assertFalse(RecurrenceExpander.occursOn(WEEKLY, 1, monday, start, until, LocalDate.parse("2026-10-06")));
    }

    @Test
    void localTimeIsKeptAcrossDaylightSaving() {
        assertEquals(LocalDateTime.parse("2027-03-14T09:00"),
                RecurrenceExpander.resolveLocal(LocalDateTime.parse("2027-03-14T09:00"), "America/Boise"));
        assertEquals(LocalDateTime.parse("2026-11-01T01:30"),
                RecurrenceExpander.resolveLocal(LocalDateTime.parse("2026-11-01T01:30"), "America/Boise"));
    }

    @Test
    void timeInSpringForwardGapMovesForward() {
        assertEquals(LocalDateTime.parse("2027-03-14T03:30"),
                RecurrenceExpander.resolveLocal(LocalDateTime.parse("2027-03-14T02:30"), "America/Boise"));
    }

    private static List<LocalDate> expand(RecurrenceFrequency frequency, int interval, Set<DayOfWeek> days,
                                          String start, String until, String windowStart, String windowEnd) {
        return RecurrenceExpander.occurrenceDates(frequency, interval, days, LocalDate.parse(start),
                until == null ? null : LocalDate.parse(until), LocalDate.parse(windowStart), LocalDate.parse(windowEnd));
    }

    private static List<LocalDate> dates(String... values) {
        return Arrays.stream(values).map(LocalDate::parse).toList();
    }
}
