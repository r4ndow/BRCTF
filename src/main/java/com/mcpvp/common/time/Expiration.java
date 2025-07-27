package com.mcpvp.common.time;

public class Expiration {

    private long expiration = 0;

    public Expiration expireIn(long milliseconds) {
        expiration = System.currentTimeMillis() + milliseconds;
        return this;
    }

    public Expiration expireIn(Duration duration) {
        return expireIn(duration.toMilliseconds());
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiration;
    }

    public Expiration expireNow() {
        expiration = 0;
        return this;
    }

    /**
     * @return A Duration spanning the length of time remaining.
     */
    public Duration getRemaining() {
        return Duration.ms(expiration - System.currentTimeMillis());
    }

}
