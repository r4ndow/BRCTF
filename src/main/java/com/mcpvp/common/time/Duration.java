package com.mcpvp.common.time;

import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A duration is a "one fits all" solution for saving an amount of time, being able to convert from and to {@link Unit}s
 * A duration will take a double, but is parsed to a long amount of milliseconds.
 */
public class Duration implements Serializable {

    public static final Duration ZERO = new Duration(0, Unit.MILLISECOND);

    public enum Unit {
        MILLISECOND(1, "ms"),
        TICK(50, "t"),
        SECOND(1000, "s"),
        MINUTE(60000, "m"),
        HOUR(3600000, "h");

        @Getter
        final long milliseconds;
        final String symbol;
        final Pattern pattern;

        Unit(long milliseconds, String symbol) {
            this.milliseconds = milliseconds;
            this.symbol = symbol;
            this.pattern = Pattern.compile("(\\d+)" + symbol + "(?:\\s|$|\\d)");
        }

    }

    private final long milliseconds;

    public Duration(long value, Unit unit) {
        milliseconds = (value * unit.getMilliseconds());
    }

    public Duration(double value, Unit unit) {
        milliseconds = (long) (value * unit.getMilliseconds());
    }

    public Duration(Date startTime, Date endTime) {
        milliseconds = endTime.getTime() - startTime.getTime();
    }

    public long toMilliseconds() {
        return milliseconds;
    }

    public long ms() {
        return toMilliseconds();
    }

    public int toTicks() {
        return (int) getValue(Unit.TICK);
    }

    public int ticks() {
        return toTicks();
    }

    public long toSeconds() {
        return getValue(Unit.SECOND);
    }

    public long seconds() {
        return toSeconds();
    }

    public long toMinutes() {
        return getValue(Unit.MINUTE);
    }

    public long mins() {
        return toMinutes();
    }

    public long toHours() {
        return getValue(Unit.HOUR);
    }

    public double toHoursExact() {
        return (double) ms() / Unit.HOUR.getMilliseconds();
    }

    public long hrs() {
        return toHours();
    }

    public long getValue(Unit unit) {
        return toMilliseconds() / unit.getMilliseconds();
    }

    public String formatText() {
        return formatText(Unit.MILLISECOND);
    }

    public String formatText(Unit smallest) {
        long ms = milliseconds;
        List<String> strings = new LinkedList<>();

        for (int i = Unit.values().length - 1; i != -1 && ms > 0; i--) {
            if (smallest != null && i < smallest.ordinal()) {
                continue;
            }

            Unit u = Unit.values()[i];
            long v = ms(ms).getValue(u);

            if (v != 0) {
                strings.add(v + u.symbol);
                ms -= new Duration(v, u).ms();
            }
        }

        return String.join(" ", strings);
    }

    /**
     * Add this duration to the parameter into a new Duration.
     *
     * @param duration
     * @return A new duration with the sum of this and <b>duration</b>
     */
    public Duration add(Duration duration) {
        return Duration.milliseconds(this.toMilliseconds() + duration.toMilliseconds());
    }

    /**
     * Subtracts the parameter from this into a new Duration.
     *
     * @param duration
     * @return A new duration with the difference of {@code this} and {@code duration}.
     */
    public Duration subtract(Duration duration) {
        return Duration.milliseconds(this.toMilliseconds() - duration.toMilliseconds());
    }

    /**
     * Multiplies this and the parameter into a new Duration.
     *
     * @param duration
     * @return A new duration with the product of {@code this} and {@code duration}.
     */
    public Duration multiply(Duration duration) {
        return Duration.milliseconds(this.toMilliseconds() * duration.toMilliseconds());
    }

    /**
     * Divides this by the parameter into a new Duration.
     *
     * @param duration
     * @return A new duration with the dividend of {@code this} and {@code duration}.
     */
    public Duration divide(Duration duration) {
        return Duration.milliseconds(this.toMilliseconds() / duration.toMilliseconds());
    }

    public java.time.Duration toJava() {
        return java.time.Duration.ofMillis(milliseconds);
    }

    public static Duration milliseconds(double milliseconds) {
        return new Duration(milliseconds, Unit.MILLISECOND);
    }

    public static Duration milliseconds(long milliseconds) {
        return new Duration(milliseconds, Unit.MILLISECOND);
    }

    public static Duration ms(double milliseconds) {
        return milliseconds(milliseconds);
    }

    public static Duration ms(long milliseconds) {
        return milliseconds(milliseconds);
    }

    public static Duration ticks(double ticks) {
        return new Duration(ticks, Unit.TICK);
    }

    public static Duration ticks(long ticks) {
        return new Duration(ticks, Unit.TICK);
    }

    public static Duration t(double ticks) {
        return ticks(ticks);
    }

    public static Duration t(long ticks) {
        return ticks(ticks);
    }

    public static Duration seconds(double seconds) {
        return new Duration(seconds, Unit.SECOND);
    }

    public static Duration seconds(long seconds) {
        return new Duration(seconds, Unit.SECOND);
    }

    public static Duration secs(double seconds) {
        return seconds(seconds);
    }

    public static Duration secs(long seconds) {
        return seconds(seconds);
    }

    public static Duration minutes(double minutes) {
        return new Duration(minutes, Unit.MINUTE);
    }

    public static Duration minutes(long minutes) {
        return new Duration(minutes, Unit.MINUTE);
    }

    public static Duration mins(double minutes) {
        return minutes(minutes);
    }

    public static Duration mins(long minutes) {
        return minutes(minutes);
    }

    public static Duration hours(double hours) {
        return new Duration(hours, Unit.HOUR);
    }

    public static Duration hours(long hours) {
        return new Duration(hours, Unit.HOUR);
    }

    public static Duration hrs(double hours) {
        return hours(hours);
    }

    public static Duration hrs(long hours) {
        return hours(hours);
    }

    /**
     * @param time The time to measure between.
     * @return A Duration that is equal to the length of time between right now
     * and the given Timestamp.
     */
    public static Duration since(LocalDateTime time) {
        long t = time.toInstant(ZoneOffset.UTC).toEpochMilli();
        if (t > System.currentTimeMillis()) {
            return ms(t - System.currentTimeMillis());
        }

        return ms(System.currentTimeMillis() - t);
    }

    /**
     * @param ms The time to measure between.
     * @return A Duration that is equal to the length of time between right now
     * and the given Timestamp.
     */
    public static Duration since(long ms) {
        return ms(System.currentTimeMillis() - ms);
    }

    /**
     * Parses the value of the given String. For example, {@code "10m 15ms"} or
     * {@code "10m15ms"} becomes a duration of 10 minutes and 15 ms.
     *
     * @param str The string to evaluate.
     * @return A Duration that parses the String.
     */
    public static Duration valueOf(String str) {
        Duration dur = Duration.ms(0);

        for (Unit u : Unit.values()) {
            Matcher m = u.pattern.matcher(str);

            while (m.find()) {
                dur = dur.add(new Duration(Integer.valueOf(m.group().replaceAll(m.pattern().pattern(), "$1").trim()), u));
            }
        }

        return dur;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Duration && toMilliseconds() == ((Duration) o).toMilliseconds();
    }

    @Override
    public int hashCode() {
        return Long.valueOf(toMilliseconds()).hashCode();
    }

    @Override
    public String toString() {
        return formatText();
    }

}
