package com.derekgelvez.calendar.model;

/** What a guest may do on someone else's calendar. The owner always has full control. */
public enum AccessLevel {
    /** View events only. */
    READ_ONLY,
    /** Also create events, and edit or delete the events this guest created. */
    READ_WRITE
}
