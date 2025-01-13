package com.mcpvp.common.time;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class Expiration {

    private long expiration = 0;

    public Expiration expireIn(long milliseconds) {
        expiration = System.currentTimeMillis() + milliseconds;
        return this;
    }

    public Expiration expireIn(Duration duration) {
        return expireIn(duration.toMilliseconds());
    }

    public Expiration expireAt(Timestamp timestamp) {
        expiration = timestamp.getTime();
        return this;
    }

    public Expiration expireAt(LocalDateTime timestamp) {
        expiration = timestamp.toInstant(ZoneOffset.UTC).toEpochMilli();
        return this;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiration;
    }

    public Expiration expireNow() {
        expiration = 0;
        return this;
    }

    public long getExpiration() {
        return expiration;
    }

    /**
     * @return A Duration spanning the length of time remaining.
     */
    public Duration getRemaining() {
        return Duration.ms(expiration - System.currentTimeMillis());
    }
}
