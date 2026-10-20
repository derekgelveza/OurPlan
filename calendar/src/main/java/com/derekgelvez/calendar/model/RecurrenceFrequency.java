package com.derekgelvez.calendar.model;

/** How often a repeating event recurs. The exact date rules are in RecurrenceExpander. */
public enum RecurrenceFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    /** Weekly recurrence with an explicit daysOfWeek selection; expanded the same way as WEEKLY. */
    CUSTOM
}
