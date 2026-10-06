package com.derekgelvez.calendar.model;

public enum RecurrenceFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    /** Weekly recurrence with an explicit daysOfWeek selection; expanded the same way as WEEKLY. */
    CUSTOM
}
