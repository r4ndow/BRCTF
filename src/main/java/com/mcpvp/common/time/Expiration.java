package com.mcpvp.common.time;

public class Expiration {

    private long expiration = 0;

    /**
     * Constructs a new Expiration that expires after the given Duration.
     *
     * @param duration The duration after which to expire this.
     * @return The new Expiration.
     */
    public static Expiration after(Duration duration) {
        return new Expiration().expireIn(duration);
    }

    /**
     * @param duration The duration after which to expire this.
     * @return This same Expiration.
     */
    public Expiration expireIn(Duration duration) {
        this.expiration = System.currentTimeMillis() + duration.toMilliseconds();
        return this;
    }

    /**
     * @return If this has expired, eg the expiration timestamp has passed.
     */
    public boolean isExpired() {
        return System.currentTimeMillis() >= this.expiration;
    }

    /**
     * Expire this immediately such that {@link #isExpired()} returns true.
     */
    public void expireNow() {
        this.expiration = 0;
    }

    /**
     * @return A Duration spanning the length of time remaining.
     */
    public Duration getRemaining() {
        return Duration.ms(this.expiration - System.currentTimeMillis());
    }

    /**
     * @param total The total amount of time that this Expiration is based on.
     * @return The percent done from [0, 1.0].
     */
    public float getCompletionPercent(Duration total) {
        return Math.min(1, 1 - ((float) this.getRemaining().ticks() / total.ticks()));
    }

}
