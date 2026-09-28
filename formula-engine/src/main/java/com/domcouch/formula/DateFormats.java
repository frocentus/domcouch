package com.domcouch.formula;

import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Objects;

/**
 * How time-date values are shown as text ({@code @Text}, string concatenation, the host's
 * display) and which text they are parsed from: the equivalent of the Notes client's regional
 * settings.
 * <p>
 * {@link #US} (the default) keeps the engine's historic format {@code MM/dd/yyyy hh:mm:ss a}.
 * A host sets its locale once at startup:
 * <pre>{@code
 * DateFormats.setDefault(DateFormats.of(Locale.forLanguageTag("de-AT")));   // 15.03.2024 10:20:00
 * }</pre>
 * Text in the US format and ISO-8601 is always parsed as well, so stored values stay readable.
 *
 * @param locale      locale of month/day names and AM/PM markers
 * @param datePattern date part, e.g. {@code dd.MM.yyyy}
 * @param timePattern time part, e.g. {@code HH:mm:ss}
 */
public record DateFormats(Locale locale, String datePattern, String timePattern) {

    /** The engine's historic format: {@code 03/15/2024 10:20:00 AM}. */
    public static final DateFormats US = new DateFormats(Locale.US, "MM/dd/yyyy", "hh:mm:ss a");

    private static volatile DateFormats defaultFormats = US;

    public DateFormats {
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(datePattern, "datePattern");
        Objects.requireNonNull(timePattern, "timePattern");
    }

    /**
     * The locale's medium date and time style with a four-digit year, e.g. {@code dd.MM.yyyy} and
     * {@code HH:mm:ss} for {@code de-AT}. {@link Locale#US} returns {@link #US}.
     */
    public static DateFormats of(Locale locale) {
        if (Locale.US.equals(locale)) return US;
        String date = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
                FormatStyle.MEDIUM, null, IsoChronology.INSTANCE, locale);
        String time = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
                null, FormatStyle.MEDIUM, IsoChronology.INSTANCE, locale);
        return new DateFormats(locale, fourDigitYear(date), time);
    }

    /** The formats used by {@link DateTimeValue#toString()} and date parsing. */
    public static DateFormats getDefault() {
        return defaultFormats;
    }

    /** Set the formats for the whole JVM (Notes: the regional settings of the client or server). */
    public static void setDefault(DateFormats formats) {
        defaultFormats = Objects.requireNonNull(formats, "formats");
    }

    DateTimeFormatter date() {
        return DateTimeFormatter.ofPattern(datePattern, locale);
    }

    DateTimeFormatter time() {
        return DateTimeFormatter.ofPattern(timePattern, locale);
    }

    DateTimeFormatter dateTime() {
        return DateTimeFormatter.ofPattern(datePattern + " " + timePattern, locale);
    }

    /** Date without the year ({@code @Text(...; "D1")}, {@code "D2"}): {@code dd.MM}, {@code MM/dd}. */
    DateTimeFormatter dateWithoutYear() {
        return DateTimeFormatter.ofPattern(datePattern.replaceAll("[^A-Za-z']*y+[^A-Za-z']*$|^y+[^A-Za-z']*", ""), locale);
    }

    /** Year and month ({@code @Text(...; "D3")}): {@code MM.yyyy}. */
    DateTimeFormatter yearMonth() {
        return DateTimeFormatter.ofPattern(datePattern.replaceAll("d+[^A-Za-z']*|[^A-Za-z']*d+$", ""), locale);
    }

    /** Time without seconds ({@code @Text(...; "T1")}): {@code HH:mm}. */
    DateTimeFormatter timeWithoutSeconds() {
        return DateTimeFormatter.ofPattern(timePattern.replaceAll("[^A-Za-z']*s+", ""), locale);
    }

    private static String fourDigitYear(String pattern) {
        return pattern.replaceAll("y+", "yyyy");
    }
}
