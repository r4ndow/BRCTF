package com.mcpvp.common.event;

import com.mcpvp.common.time.Duration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TickEvent extends EasyEvent {

    private final long tick;

    /**
     * Utility method for using TickEvents as a way to determine when an interval
     * of time has passed. For example, when regenerating items every given duration of time.
     *
     * @param duration The duration to check.
     * @return True if this tick event is evenly divisible by the duration.
     */
    public boolean isInterval(Duration duration) {
        return tick % duration.ticks() == 0;
    }

}
