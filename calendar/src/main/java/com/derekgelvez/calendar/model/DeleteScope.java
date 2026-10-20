package com.derekgelvez.calendar.model;

/** Which occurrences of a repeating event a delete, or an edit, applies to. */
public enum DeleteScope {
    /** Only the selected date (creates an EventException). */
    THIS_OCCURRENCE,
    /** The selected date and every later date (ends the series the day before). */
    THIS_AND_FOLLOWING,
    /** The entire series and its exceptions. */
    ALL
}
