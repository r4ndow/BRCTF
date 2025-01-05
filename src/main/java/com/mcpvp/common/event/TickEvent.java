package com.mcpvp.common.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TickEvent extends EasyEvent {

    private final long tick;

}
