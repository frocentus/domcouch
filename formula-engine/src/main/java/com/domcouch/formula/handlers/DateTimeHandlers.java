package com.domcouch.formula.handlers;

import com.domcouch.formula.ContextNotSupportedException;
import com.domcouch.formula.DateTimeValue;
import com.domcouch.formula.Evaluator;
import com.domcouch.formula.FunctionHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Date/time @Function handlers.
 */
public final class DateTimeHandlers {
    private DateTimeHandlers() {}

    public static void register(java.util.Map<String, FunctionHandler> functions) {
        functions.put("BUSINESSDAYS", (ev, args, ctx) -> {
            Object starts = ev.eval(args.get(0), ctx), ends = ev.eval(args.get(1), ctx);
            HashSet<Integer> excludeDays = new HashSet<>();
            if (args.size() > 2) for (Object o : Evaluator.toList(ev.eval(args.get(2), ctx))) { int d = (int) Evaluator.toNumber(o); if (d >= 1 && d <= 7) excludeDays.add(d); }
            HashSet<java.time.LocalDate> excludeDates = new HashSet<>();
            if (args.size() > 3) for (Object o : Evaluator.toList(ev.eval(args.get(3), ctx))) { java.time.ZonedDateTime z = Evaluator.toZoned(o); if (z != null) excludeDates.add(z.toLocalDate()); }
            List<Object> startList = Evaluator.toList(starts), endList = Evaluator.toList(ends);
            int size = Math.max(startList.size(), endList.size()); List<Object> result = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                java.time.ZonedDateTime sz = Evaluator.toZoned(startList.get(Math.min(i, startList.size() - 1)));
                java.time.ZonedDateTime ez = Evaluator.toZoned(endList.get(Math.min(i, endList.size() - 1)));
                java.time.LocalDate s = sz == null ? null : sz.toLocalDate(), e = ez == null ? null : ez.toLocalDate();
                if (s == null || e == null || e.isBefore(s)) { result.add(-1.0); continue; }
                long days = 0;
                for (java.time.LocalDate d = s; !d.isAfter(e); d = d.plusDays(1)) {
                    int dow = d.getDayOfWeek().getValue(), dominoDow = (dow == 7) ? 1 : dow + 1;
                    if (!excludeDays.contains(dominoDow) && !excludeDates.contains(d)) days++;
                }
                result.add(days < 0 ? -1.0 : (double) days);
            }
            return result.size() == 1 ? result.getFirst() : result;
        });
        functions.put("DATE", (ev, args, ctx) -> {
            Object first = ev.eval(args.get(0), ctx);
            if (args.size() >= 3 && first instanceof Number) {
                int y = (int) Evaluator.toNumber(first), mo = (int) Evaluator.toNumber(ev.eval(args.get(1), ctx)), d = (int) Evaluator.toNumber(ev.eval(args.get(2), ctx));
                int h = args.size() >= 6 ? (int) Evaluator.toNumber(ev.eval(args.get(3), ctx)) : 0, mi = args.size() >= 6 ? (int) Evaluator.toNumber(ev.eval(args.get(4), ctx)) : 0, s = args.size() >= 6 ? (int) Evaluator.toNumber(ev.eval(args.get(5), ctx)) : 0;
                return args.size() >= 6 ? DateTimeValue.of(java.time.LocalDateTime.of(y, mo, d, h, mi, s)) : DateTimeValue.ofDate(java.time.LocalDate.of(y, mo, d));
            }
            List<Object> sources = Evaluator.toList(ev.eval(args.get(0), ctx)); List<Object> result = new ArrayList<>();
            for (Object src : sources) { java.time.ZonedDateTime zdt = Evaluator.toZoned(src); result.add(zdt != null ? DateTimeValue.ofDate(zdt.toLocalDate()) : ""); }
            return result.size() == 1 ? result.getFirst() : result;
        });
        functions.put("CREATED", (ev, args, ctx) -> ctx.resolve("CREATED"));
        functions.put("MODIFIED", (ev, args, ctx) -> ctx.resolve("MODIFIED"));
        functions.put("ACCESSED", (ev, args, ctx) -> ctx.resolve("ACCESSED"));
        functions.put("ADDEDTOTHISFILE", (ev, args, ctx) -> ctx.resolve("ADDEDTOTHISFILE"));
        functions.put("NOW", (ev, args, ctx) -> DateTimeValue.now());
        functions.put("TODAY", (ev, args, ctx) -> DateTimeValue.ofDate(java.time.LocalDate.now()));
        functions.put("ADJUST", (ev, args, ctx) -> {
            Object dateVal = ev.eval(args.get(0), ctx);
            int years = (int) Evaluator.toNumber(ev.eval(args.get(1), ctx)), months = (int) Evaluator.toNumber(ev.eval(args.get(2), ctx)), days = (int) Evaluator.toNumber(ev.eval(args.get(3), ctx));
            int hours = (int) Evaluator.toNumber(ev.eval(args.get(4), ctx)), minutes = (int) Evaluator.toNumber(ev.eval(args.get(5), ctx)), seconds = (int) Evaluator.toNumber(ev.eval(args.get(6), ctx));
            boolean inLocalTime = args.size() > 7 && "INLOCALTIME".equalsIgnoreCase(Evaluator.convertToString(ev.eval(args.get(7), ctx)));
            List<Object> sources = Evaluator.toList(dateVal); List<Object> result = new ArrayList<>();
            for (Object src : sources) {
                DateTimeValue value = DateTimeValue.from(src);
                java.time.ZonedDateTime dt = Evaluator.toZoned(src);
                if (value == null || dt == null) { result.add(""); continue; }
                dt = dt.plusSeconds(seconds).plusMinutes(minutes).plusHours(hours).plusDays(days).plusMonths(months).plusYears(years);
                if (inLocalTime) dt = dt.withZoneSameLocal(java.time.ZoneId.systemDefault());
                result.add(value.with(dt));
            }
            return result.size() == 1 ? result.getFirst() : result;
        });
        functions.put("TIME", (ev, args, ctx) -> {
            Object first = ev.eval(args.get(0), ctx);
            if (args.size() >= 3 && first instanceof Number) {
                int h = (int) Evaluator.toNumber(first), m = (int) Evaluator.toNumber(ev.eval(args.get(1), ctx)), s = args.size() > 2 ? (int) Evaluator.toNumber(ev.eval(args.get(2), ctx)) : 0;
                return DateTimeValue.ofTime(java.time.LocalTime.of(h, m, s));
            }
            List<Object> sources = Evaluator.toList(ev.eval(args.get(0), ctx)); List<Object> result = new ArrayList<>();
            for (Object src : sources) { java.time.ZonedDateTime zdt = Evaluator.toZoned(src); result.add(zdt == null ? "" : DateTimeValue.ofTime(zdt.toLocalTime())); }
            return result.size() == 1 ? result.getFirst() : result;
        });
        functions.put("TIMEMERGE", (ev, args, ctx) -> {
            List<Object> dates = Evaluator.toList(ev.eval(args.get(0), ctx)), times = Evaluator.toList(ev.eval(args.get(1), ctx));
            int size = Math.max(dates.size(), times.size()); List<Object> result = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                java.time.ZonedDateTime d = Evaluator.toZoned(dates.get(Math.min(i, dates.size() - 1)));
                java.time.ZonedDateTime t = Evaluator.toZoned(times.get(Math.min(i, times.size() - 1)));
                result.add(d == null || t == null ? "" : DateTimeValue.of(d.toLocalDate().atTime(t.toLocalTime()).atZone(d.getZone())));
            }
            return result.size() == 1 ? result.getFirst() : result;
        });
        functions.put("YEAR", (ev, args, ctx) -> (double) Evaluator.extractDateField(ev.eval(args.getFirst(), ctx), java.time.temporal.ChronoField.YEAR));
        functions.put("MONTH", (ev, args, ctx) -> (double) Evaluator.extractDateField(ev.eval(args.getFirst(), ctx), java.time.temporal.ChronoField.MONTH_OF_YEAR));
        functions.put("DAY", (ev, args, ctx) -> (double) Evaluator.extractDateField(ev.eval(args.getFirst(), ctx), java.time.temporal.ChronoField.DAY_OF_MONTH));
        functions.put("SECOND", (ev, args, ctx) -> (double) Evaluator.extractDateField(ev.eval(args.getFirst(), ctx), java.time.temporal.ChronoField.SECOND_OF_MINUTE));
        functions.put("MINUTE", (ev, args, ctx) -> (double) Evaluator.extractDateField(ev.eval(args.getFirst(), ctx), java.time.temporal.ChronoField.MINUTE_OF_HOUR));
        functions.put("HOUR", (ev, args, ctx) -> (double) Evaluator.extractDateField(ev.eval(args.getFirst(), ctx), java.time.temporal.ChronoField.HOUR_OF_DAY));
        functions.put("WEEKDAY", (ev, args, ctx) -> { long v = Evaluator.extractDateField(ev.eval(args.getFirst(), ctx), java.time.temporal.ChronoField.DAY_OF_WEEK); return v == 7 ? 1.0 : (double) (v + 1); });
        functions.put("TOMORROW", (ev, args, ctx) -> DateTimeValue.ofDate(java.time.LocalDate.now().plusDays(1)));
        functions.put("YESTERDAY", (ev, args, ctx) -> DateTimeValue.ofDate(java.time.LocalDate.now().minusDays(1)));
        functions.put("ZONE", (ev, args, ctx) -> { String td = args.isEmpty() ? null : Evaluator.convertToString(ev.eval(args.getFirst(), ctx)); try { return ctx.getTimeZoneOffset(td); } catch (
                ContextNotSupportedException e) { return java.time.ZoneId.systemDefault().getId(); } });
        functions.put("GETCURRENTTIMEZONE", (ev, args, ctx) -> { try { return ctx.getCanonicalTimeZone(); } catch (ContextNotSupportedException e) { return java.time.ZoneId.systemDefault().getId(); } });
        functions.put("TIMETOTEXTINZONE", (ev, args, ctx) -> { String td = Evaluator.convertToString(ev.eval(args.get(0), ctx)), tz = Evaluator.convertToString(ev.eval(args.get(1), ctx)), fmt = args.size() > 2 ? Evaluator.convertToString(ev.eval(args.get(2), ctx)) : ""; try { return ctx.timeToTextInZone(td, tz, fmt); } catch (ContextNotSupportedException e) { return ""; } });
        functions.put("TIMEZONETOTEXT", (ev, args, ctx) -> { String tz = Evaluator.convertToString(ev.eval(args.get(0), ctx)), fmt = args.size() > 1 ? Evaluator.convertToString(ev.eval(args.get(1), ctx)) : ""; try { return ctx.timeZoneToText(tz, fmt); } catch (ContextNotSupportedException e) { return ""; } });
    }
}
