package com.domcouch.api;

import java.util.Date;

/**
 * Domino-style DateTime abstraction.
 * Mirrors lotus.domino.DateTime.
 */
public interface DateTime {

    /**
     * @return local time as ISO-8601 string
     */
    String getLocalTime();

    /**
     * @return GMT time as ISO-8601 string
     */
    String getGMTTime();

    /**
     * @return this datetime as a java.util.Date
     */
    Date toJavaDate();

    /**
     * Compare this time with another DateTime.
     *
     * @param other the DateTime to compare to
     * @return -1 if this is earlier, 0 if equal, 1 if later
     */
    int timeDifference(DateTime other);

    /**
     * Adjust the time by a number of days.
     *
     * @param days positive or negative day offset
     */
    void adjustDay(int days);

    /**
     * Adjust the time by a number of hours.
     *
     * @param hours positive or negative hour offset
     */
    void adjustHour(int hours);

    /**
     * @return true if this DateTime represents a date-only value (no time component)
     */
    boolean isDateOnly();
}
