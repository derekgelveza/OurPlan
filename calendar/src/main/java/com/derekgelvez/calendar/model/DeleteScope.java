package com.derekgelvez.calendar.model;

public enum DeleteScope {
    /** Only the selected date (creates an EventException). */
    THIS_OCCURRENCE,
    /** The selected date and every later date (ends the series the day before). */
    THIS_AND_FOLLOWING,
    /** The entire series and its exceptions. */
    ALL
}
