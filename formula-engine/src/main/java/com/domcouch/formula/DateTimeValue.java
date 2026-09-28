package com.domcouch.formula;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * A Notes time-date value: date and time, date only ({@code @Today}, {@code @Date(y; m; d)}),
 * or time only ({@code @Time(h; m; s)}).
 * <p>
 * Formulas compare these chronologically, {@code date - date} gives the difference in seconds,
 * {@code date ± number} adds seconds, and {@link #toString()} shows the value in the
 * {@link DateFormats#getDefault() default formats}, without the part it does not have.
 * Hosts pass dates into formulas with {@link #ofDate}, {@link #of(LocalDateTime)} or
 * {@link #of(ZonedDateTime)}.
 */
public final class DateTimeValue implements Comparable<DateTimeValue> {

    /** Date of a time-only value (Notes has none; only the time part is ever shown). */
    private static final LocalDate NO_DATE = LocalDate.of(1970, 1, 1);

    private final ZonedDateTime value;
    private final boolean hasDate;
    private final boolean hasTime;

    private DateTimeValue(ZonedDateTime value, boolean hasDate, boolean hasTime) {
        this.value = Objects.requireNonNull(value, "value").truncatedTo(ChronoUnit.SECONDS);
        this.hasDate = hasDate;
        this.hasTime = hasTime;
    }

    public static DateTimeValue of(ZonedDateTime dateTime) {
        return new DateTimeValue(dateTime, true, true);
    }

    public static DateTimeValue of(LocalDateTime dateTime) {
        return of(dateTime.atZone(ZoneId.systemDefault()));
    }

    public static DateTimeValue ofDate(LocalDate date) {
        return new DateTimeValue(date.atStartOfDay(ZoneId.systemDefault()), true, false);
    }

    public static DateTimeValue ofTime(LocalTime time) {
        return new DateTimeValue(time.atDate(NO_DATE).atZone(ZoneId.systemDefault()), false, true);
    }

    /** {@code @Now}. */
    public static DateTimeValue now() {
        return of(ZonedDateTime.now());
    }

    /**
     * A formula value as time-date: a {@code DateTimeValue} itself, or text in the default formats,
     * the US format or ISO-8601 (text without a time is date only, text without a date time only).
     * Numbers and other text give {@code null}.
     */
    public static DateTimeValue from(Object value) {
        if (value instanceof DateTimeValue dt) return dt;
        if (!(value instanceof String s) || s.isBlank()) return null;
        return Evaluator.parseDateTime(s.trim());
    }

    public ZonedDateTime toZonedDateTime() {
        return value;
    }

    public LocalDate toLocalDate() {
        return value.toLocalDate();
    }

    public LocalDateTime toLocalDateTime() {
        return value.toLocalDateTime();
    }

    public LocalTime toLocalTime() {
        return value.toLocalTime();
    }

    public boolean hasDate() {
        return hasDate;
    }

    public boolean hasTime() {
        return hasTime;
    }

    public int get(ChronoField field) {
        return value.get(field);
    }

    /** The same kind of value (date only stays date only), shifted. */
    public DateTimeValue with(ZonedDateTime shifted) {
        return new DateTimeValue(shifted, hasDate, hasTime);
    }

    /** {@code date + number}: Notes adds seconds; the result has a time. */
    public DateTimeValue plusSeconds(double seconds) {
        return new DateTimeValue(value.plusSeconds(Math.round(seconds)), hasDate, true);
    }

    /** {@code date - date}: difference in seconds. */
    public double secondsSince(DateTimeValue other) {
        return ChronoUnit.SECONDS.between(other.value, value);
    }

    /**
     * ISO-8601 for storage, independent of the regional formats: {@code 2024-03-15} (date only),
     * {@code 10:20:00} (time only) or {@code 2024-03-15T10:20:00+01:00}. {@link #from} reads it back
     * as the same kind of value.
     */
    public String toIso() {
        if (hasDate && hasTime) return value.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        if (hasDate) return value.toLocalDate().toString();
        return value.toLocalTime().format(java.time.format.DateTimeFormatter.ISO_LOCAL_TIME);
    }

    public String format(DateFormats formats) {
        if (hasDate && hasTime) return formats.dateTime().format(value);
        if (hasDate) return formats.date().format(value);
        return formats.time().format(value);
    }

    /** The value in the {@link DateFormats#getDefault() default formats}. */
    @Override
    public String toString() {
        return format(DateFormats.getDefault());
    }

    @Override
    public int compareTo(DateTimeValue other) {
        if (!hasDate || !other.hasDate) return value.toLocalTime().compareTo(other.value.toLocalTime());
        return value.toInstant().compareTo(other.value.toInstant());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DateTimeValue other && hasDate == other.hasDate && hasTime == other.hasTime
                && value.toInstant().equals(other.value.toInstant());
    }

    @Override
    public int hashCode() {
        return Objects.hash(value.toInstant(), hasDate, hasTime);
    }
}
