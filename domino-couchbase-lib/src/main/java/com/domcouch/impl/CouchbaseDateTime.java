package com.domcouch.impl;

import com.domcouch.api.DateTime;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Couchbase-backed DateTime implementation.
 */
public class CouchbaseDateTime implements DateTime {

    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private static final DateTimeFormatter LOCAL_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    private ZonedDateTime zoned;
    private boolean dateOnly;

    public CouchbaseDateTime(Date date) {
        this.zoned = date.toInstant().atZone(ZoneId.systemDefault());
        this.dateOnly = false;
    }

    public CouchbaseDateTime(Instant instant) {
        this.zoned = instant.atZone(ZoneId.systemDefault());
        this.dateOnly = false;
    }

    public CouchbaseDateTime(String isoString) {
        this.zoned = ZonedDateTime.parse(isoString);
        this.dateOnly = false;
    }

    public static CouchbaseDateTime now() {
        return new CouchbaseDateTime(Instant.now());
    }

    @Override
    public String getLocalTime() {
        if (dateOnly) {
            return zoned.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return zoned.format(LOCAL_FMT);
    }

    @Override
    public String getGMTTime() {
        if (dateOnly) {
            return zoned.withZoneSameInstant(ZoneId.of("GMT"))
                    .format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return zoned.withZoneSameInstant(ZoneId.of("GMT"))
                .format(ISO_FMT);
    }

    @Override
    public Date toJavaDate() {
        return Date.from(zoned.toInstant());
    }

    @Override
    public int timeDifference(DateTime other) {
        Date d1 = toJavaDate();
        Date d2 = other.toJavaDate();
        return d1.compareTo(d2);
    }

    @Override
    public void adjustDay(int days) {
        this.zoned = zoned.plusDays(days);
    }

    @Override
    public void adjustHour(int hours) {
        this.zoned = zoned.plusHours(hours);
    }

    @Override
    public boolean isDateOnly() {
        return dateOnly;
    }

    public void setDateOnly(boolean dateOnly) {
        this.dateOnly = dateOnly;
    }

    @Override
    public String toString() {
        return getLocalTime();
    }
}
